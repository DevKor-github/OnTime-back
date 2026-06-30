#!/usr/bin/env bash
set -euo pipefail

LABEL="${1:-}"
MODE="${2:-quick}"

if [[ "$LABEL" != "before" && "$LABEL" != "after" ]]; then
  echo "usage: $0 <before|after> [quick|full]" >&2
  exit 2
fi

if [[ "$MODE" != "quick" && "$MODE" != "full" ]]; then
  echo "usage: $0 <before|after> [quick|full]" >&2
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
APP_DIR="${BENCH_APP_DIR:-$REPO_ROOT/ontime-back}"

DB_CONTAINER="${BENCH_DB_CONTAINER:-ontime-jwt-bench-mysql}"
DB_HOST="${BENCH_DB_HOST:-127.0.0.1}"
DB_PORT="${BENCH_DB_PORT:-3308}"
DB_NAME="${BENCH_DB_NAME:-ontime_jwt_bench}"
DB_USER="${BENCH_DB_USER:-ontime_jwt_bench}"
DB_PASSWORD="${BENCH_DB_PASSWORD:-ontime_jwt_bench_password}"
DB_ROOT_PASSWORD="${BENCH_DB_ROOT_PASSWORD:-ontime_jwt_bench_root_password}"

BACKEND_PORT="${BENCH_BACKEND_PORT:-18082}"
BACKEND_URL="http://127.0.0.1:${BACKEND_PORT}"

RUNS="${RUNS:-}"
if [[ -z "$RUNS" ]]; then
  if [[ "$MODE" == "full" ]]; then
    RUNS=10
  else
    RUNS=1
  fi
fi

WARMUP_REQUESTS="${WARMUP_REQUESTS:-20}"
MEASUREMENT_DURATION="${MEASUREMENT_DURATION:-5m}"
SCENARIOS="${SCENARIOS:-1 10 20}"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
RESULT_DIR="${RESULT_DIR:-$SCRIPT_DIR/results/${TIMESTAMP}-${LABEL}-${MODE}}"
mkdir -p "$RESULT_DIR"

BACKEND_PID=""
ACCESS_TOKEN=""

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 1
  fi
}

require_docker_daemon() {
  if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon is not running. Start Docker Desktop and retry." >&2
    exit 1
  fi
}

cleanup() {
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
    wait "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

wait_for_mysql() {
  for _ in {1..90}; do
    if mysqladmin ping -h"$DB_HOST" -P"$DB_PORT" -uroot -p"$DB_ROOT_PASSWORD" --silent >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "MySQL did not become ready" >&2
  exit 1
}

wait_for_http() {
  local url="$1"
  local name="$2"
  local log_path="${3:-}"
  for _ in {1..180}; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "$name did not become ready at $url" >&2
  if [[ -n "$log_path" && -f "$log_path" ]]; then
    tail -n 120 "$log_path" >&2 || true
  fi
  exit 1
}

reset_database() {
  mysql -h"$DB_HOST" -P"$DB_PORT" -uroot -p"$DB_ROOT_PASSWORD" <<SQL
DROP DATABASE IF EXISTS ${DB_NAME};
CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL
}

start_mysql() {
  if docker ps -a --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
    docker start "$DB_CONTAINER" >/dev/null
  else
    docker run -d \
      --name "$DB_CONTAINER" \
      -e MYSQL_DATABASE="$DB_NAME" \
      -e MYSQL_USER="$DB_USER" \
      -e MYSQL_PASSWORD="$DB_PASSWORD" \
      -e MYSQL_ROOT_PASSWORD="$DB_ROOT_PASSWORD" \
      -e TZ=Asia/Seoul \
      -p "127.0.0.1:${DB_PORT}:3306" \
      mysql:8.0 \
      --character-set-server=utf8mb4 \
      --collation-server=utf8mb4_unicode_ci \
      --default-time-zone=+09:00 >/dev/null
  fi
  wait_for_mysql
  reset_database
}

start_backend() {
  (
    cd "$APP_DIR"
    SPRING_PROFILES_ACTIVE=bench \
    SERVER_PORT="$BACKEND_PORT" \
    SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true" \
    SPRING_DATASOURCE_USERNAME="$DB_USER" \
    SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
    JWT_SECRET_KEY="bench_secret_key_for_ontime_back_application_benchmark_environment_1234567890" \
    APPLE_CLIENT_ID="club.devkor.ontime.bench" \
    APPLE_TEAM_ID="BENCHTEAM1" \
    APPLE_LOGIN_KEY="BENCHKEY1" \
    APPLE_PRIVATE_KEY_BASE64="jwt-auth-benchmark-does-not-use-apple-login" \
    FEATURE_APPLE_LOGIN_ENABLED=false \
      ./gradlew bootRun
  ) > "$RESULT_DIR/backend.log" 2>&1 &
  BACKEND_PID="$!"
  wait_for_http "$BACKEND_URL/health" "backend" "$RESULT_DIR/backend.log"
}

create_benchmark_user() {
  local email="jwt-bench-${TIMESTAMP}@example.com"
  local payload
  payload="$(printf '{"email":"%s","password":"Bench123!","name":"jwt-bench-%s"}' "$email" "$TIMESTAMP")"

  curl -fsS \
    -D "$RESULT_DIR/signup-headers.txt" \
    -o "$RESULT_DIR/signup-body.json" \
    -H "content-type: application/json" \
    -X POST "$BACKEND_URL/sign-up" \
    --data "$payload"

  ACCESS_TOKEN="$(awk 'tolower($1) == "authorization:" {print $2}' "$RESULT_DIR/signup-headers.txt" | tr -d '\r' | tail -n 1)"
  if [[ -z "$ACCESS_TOKEN" ]]; then
    echo "sign-up did not return an Authorization header" >&2
    cat "$RESULT_DIR/signup-headers.txt" >&2
    cat "$RESULT_DIR/signup-body.json" >&2
    exit 1
  fi

  printf '%s\n' "$ACCESS_TOKEN" > "$RESULT_DIR/access-token.txt"
}

warmup_endpoint() {
  for _ in $(seq 1 "$WARMUP_REQUESTS"); do
    curl -fsS \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      "$BACKEND_URL/users/me/punctuality-score" >/dev/null
  done
}

measure_sql_budget_sample() {
  local output_path="$RESULT_DIR/sql-budget-${LABEL}.txt"

  mysql -h"$DB_HOST" -P"$DB_PORT" -uroot -p"$DB_ROOT_PASSWORD" <<SQL >/dev/null
SET GLOBAL log_output = 'TABLE';
SET GLOBAL general_log = 'ON';
TRUNCATE TABLE mysql.general_log;
SQL

  curl -fsS \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "$BACKEND_URL/users/me/punctuality-score" >/dev/null

  mysql -N -h"$DB_HOST" -P"$DB_PORT" -uroot -p"$DB_ROOT_PASSWORD" <<SQL > "$output_path"
SELECT COUNT(*)
FROM mysql.general_log
WHERE user_host LIKE '${DB_USER}%'
  AND command_type IN ('Query', 'Execute')
  AND argument REGEXP '^[[:space:]]*(select|insert|update|delete)';
SQL

  mysql -h"$DB_HOST" -P"$DB_PORT" -uroot -p"$DB_ROOT_PASSWORD" <<SQL >/dev/null
SET GLOBAL general_log = 'OFF';
SQL
}

run_k6_once() {
  local concurrency="$1"
  local run_number="$2"
  local run_id
  run_id="$(printf 'run%02d' "$run_number")"

  K6_VUS="$concurrency" \
  K6_DURATION="$MEASUREMENT_DURATION" \
  BACKEND_URL="$BACKEND_URL" \
  ACCESS_TOKEN="$ACCESS_TOKEN" \
  K6_RESULT_JSON="$RESULT_DIR/k6-${LABEL}-c${concurrency}-${run_id}.json" \
    k6 run "$SCRIPT_DIR/jwt-auth.k6.js" > "$RESULT_DIR/k6-${LABEL}-c${concurrency}-${run_id}.log"
}

main() {
  require_command docker
  require_command mysql
  require_command mysqladmin
  require_command curl
  require_command k6
  require_docker_daemon

  start_mysql
  start_backend
  create_benchmark_user
  warmup_endpoint
  measure_sql_budget_sample

  for concurrency in $SCENARIOS; do
    for run_number in $(seq 1 "$RUNS"); do
      echo "running ${LABEL}/${MODE}: concurrency=${concurrency}, run=${run_number}/${RUNS}"
      run_k6_once "$concurrency" "$run_number"
    done
  done

  python3 "$SCRIPT_DIR/summarize.py" "$RESULT_DIR"
}

main
