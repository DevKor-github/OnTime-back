# JWT Auth Benchmark

This benchmark measures issue #325: duplicate `User` reads on the protected API JWT authentication path.

## Scenario

- Endpoint: `GET /users/me/punctuality-score`
- Primary path: valid access token for the current Active Session
- User setup: created through `POST /sign-up` in the benchmark database
- Primary gate: authentication `User` lookup is covered by automated tests
- Benchmark evidence: protected request p50/p95/p99 latency and whole-request SQL budget sample

## Run

Install `k6` first if it is not already available:

```bash
brew install k6
```

Run from the repository root:

```bash
scripts/benchmarks/jwt-auth/run.sh before quick
scripts/benchmarks/jwt-auth/run.sh after quick
```

Full evidence runs:

```bash
scripts/benchmarks/jwt-auth/run.sh before full
scripts/benchmarks/jwt-auth/run.sh after full
```

The script starts an isolated MySQL container, starts the backend with the `bench` Spring profile, creates a benchmark User, runs k6, and writes results.

The script does not run `git checkout`. Measure the checkout you have selected, and use `before` or `after` only as the result label.

## Defaults

- Database container: `ontime-jwt-bench-mysql`
- Database: `ontime_jwt_bench`
- MySQL port: `127.0.0.1:3308`
- Backend port: `127.0.0.1:18082`
- Quick mode: 1 run per concurrency
- Full mode: 10 runs per concurrency
- Warmup: 20 requests
- Measurement: 5 minutes
- Concurrency: 1, 10, 20

Override example:

```bash
WARMUP_REQUESTS=5 MEASUREMENT_DURATION=30s SCENARIOS="1" RUNS=1 \
  scripts/benchmarks/jwt-auth/run.sh before quick
```

## Results

Results are written under:

```text
scripts/benchmarks/jwt-auth/results/<timestamp>-<label>-<mode>/
```

Each result directory contains:

- `k6-*.json`: k6 measurement summary
- `summary.csv`: joined p50/p95/p99 and error rate
- `sql-budget-*.txt`: MySQL general-log sample for one protected request
- `backend.log`: backend logs for the run
- `signup-headers.txt`, `signup-body.json`, `access-token.txt`: benchmark User setup artifacts

Use `docs/performance/jwt-auth-325.md` for the human-readable comparison table.

## Acceptance Gate

Primary correctness gate:

- `AuthTokenServiceTest` verifies Active Session validation performs exactly one `UserRepository.findById(...)`.
- `JwtAuthenticationQueryCountIntegrationTest` verifies `GET /users/me/punctuality-score` stays within the post-fix whole-request SQL budget.

Benchmark evidence:

- Error rate: `0%`
- p95 latency: `after <= before`
- Whole-request SQL budget sample: `after <= before - 1`
