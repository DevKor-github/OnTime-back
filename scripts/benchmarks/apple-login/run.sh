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
APP_DIR="$REPO_ROOT/ontime-back"

DB_CONTAINER="${BENCH_DB_CONTAINER:-ontime-bench-mysql}"
DB_HOST="${BENCH_DB_HOST:-127.0.0.1}"
DB_PORT="${BENCH_DB_PORT:-3307}"
DB_NAME="${BENCH_DB_NAME:-ontime_bench}"
DB_USER="${BENCH_DB_USER:-ontime_bench}"
DB_PASSWORD="${BENCH_DB_PASSWORD:-ontime_bench_password}"
DB_ROOT_PASSWORD="${BENCH_DB_ROOT_PASSWORD:-ontime_bench_root_password}"

BACKEND_PORT="${BENCH_BACKEND_PORT:-18081}"
STUB_PORT="${APPLE_STUB_PORT:-18080}"
BACKEND_URL="http://127.0.0.1:${BACKEND_PORT}"
APPLE_STUB_URL="http://127.0.0.1:${STUB_PORT}"
APPLE_CLIENT_ID="${APPLE_CLIENT_ID:-club.devkor.ontime.bench}"

RUNS="${RUNS:-}"
if [[ -z "$RUNS" ]]; then
  if [[ "$MODE" == "full" ]]; then
    RUNS=10
  else
    RUNS=1
  fi
fi

WARMUP_DURATION="${WARMUP_DURATION:-2m}"
MEASUREMENT_DURATION="${MEASUREMENT_DURATION:-5m}"
SCENARIOS="${SCENARIOS:-1 10 20}"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
RESULT_DIR="${RESULT_DIR:-$SCRIPT_DIR/results/${TIMESTAMP}-${LABEL}-${MODE}}"
mkdir -p "$RESULT_DIR"

BACKEND_PID=""
STUB_PID=""

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
  if [[ -n "$STUB_PID" ]] && kill -0 "$STUB_PID" >/dev/null 2>&1; then
    kill "$STUB_PID" >/dev/null 2>&1 || true
    wait "$STUB_PID" >/dev/null 2>&1 || true
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
    if [[ -n "$log_path" && ! -s "$log_path" ]]; then
      true
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

seed_returning_user() {
  mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$SCRIPT_DIR/seed-returning-user.sql"
}

generate_private_key_base64() {
  node - <<'NODE'
const { generateKeyPairSync } = require("node:crypto");
const { privateKey } = generateKeyPairSync("ec", { namedCurve: "P-256" });
const pem = privateKey.export({ type: "pkcs8", format: "pem" });
process.stdout.write(Buffer.from(pem).toString("base64"));
NODE
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

start_stub() {
  APPLE_STUB_PORT="$STUB_PORT" \
  APPLE_CLIENT_ID="$APPLE_CLIENT_ID" \
  BENCH_APPLE_SUB="bench-apple-user" \
  APPLE_KEYS_DELAY_MS=80 \
  APPLE_EXCHANGE_DELAY_MS=300 \
    node "$SCRIPT_DIR/apple-stub.mjs" > "$RESULT_DIR/apple-stub.log" 2>&1 &
  STUB_PID="$!"
  wait_for_http "$APPLE_STUB_URL/fixture/apple-login-payload" "Apple stub" "$RESULT_DIR/apple-stub.log"
}

start_backend() {
  local private_key_base64
  private_key_base64="$(generate_private_key_base64)"
  (
    cd "$APP_DIR"
    SPRING_PROFILES_ACTIVE=bench \
    SERVER_PORT="$BACKEND_PORT" \
    SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true" \
    SPRING_DATASOURCE_USERNAME="$DB_USER" \
    SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
    JWT_SECRET_KEY="bench_secret_key_for_ontime_back_application_benchmark_environment_1234567890" \
    APPLE_CLIENT_ID="$APPLE_CLIENT_ID" \
    APPLE_TEAM_ID="BENCHTEAM1" \
    APPLE_LOGIN_KEY="BENCHKEY1" \
    APPLE_PRIVATE_KEY_BASE64="$private_key_base64" \
    APPLE_KEYS_URL="$APPLE_STUB_URL/auth/keys" \
    APPLE_TOKEN_URL="$APPLE_STUB_URL/auth/token" \
      ./gradlew bootRun
  ) > "$RESULT_DIR/backend.log" 2>&1 &
  BACKEND_PID="$!"
  wait_for_http "$BACKEND_URL/health" "backend" "$RESULT_DIR/backend.log"
  seed_returning_user
}

run_k6_once() {
  local concurrency="$1"
  local run_number="$2"
  local run_id
  run_id="$(printf 'run%02d' "$run_number")"

  K6_VUS="$concurrency" \
  K6_DURATION="$WARMUP_DURATION" \
  BACKEND_URL="$BACKEND_URL" \
  APPLE_STUB_URL="$APPLE_STUB_URL" \
  K6_RESULT_JSON="$RESULT_DIR/warmup-${LABEL}-c${concurrency}-${run_id}.json" \
    k6 run "$SCRIPT_DIR/apple-login.k6.js" > "$RESULT_DIR/warmup-${LABEL}-c${concurrency}-${run_id}.log"

  curl -fsS -X POST "$APPLE_STUB_URL/__reset" >/dev/null

  K6_VUS="$concurrency" \
  K6_DURATION="$MEASUREMENT_DURATION" \
  BACKEND_URL="$BACKEND_URL" \
  APPLE_STUB_URL="$APPLE_STUB_URL" \
  K6_RESULT_JSON="$RESULT_DIR/k6-${LABEL}-c${concurrency}-${run_id}.json" \
    k6 run "$SCRIPT_DIR/apple-login.k6.js" > "$RESULT_DIR/k6-${LABEL}-c${concurrency}-${run_id}.log"

  curl -fsS "$APPLE_STUB_URL/__counts" > "$RESULT_DIR/stub-${LABEL}-c${concurrency}-${run_id}.json"
}

main() {
  require_command docker
  require_command mysql
  require_command mysqladmin
  require_command curl
  require_command node
  require_command k6
  require_docker_daemon

  start_mysql
  start_stub
  start_backend

  for concurrency in $SCENARIOS; do
    for run_number in $(seq 1 "$RUNS"); do
      echo "running ${LABEL}/${MODE}: concurrency=${concurrency}, run=${run_number}/${RUNS}"
      run_k6_once "$concurrency" "$run_number"
    done
  done

  python3 "$SCRIPT_DIR/summarize.py" "$RESULT_DIR"
}

main
