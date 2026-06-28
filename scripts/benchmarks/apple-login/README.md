# Apple Login Benchmark

This benchmark measures issue #323: repeated Apple provider round trips on the returning Apple User login path.

## Scenario

- Endpoint: `POST /oauth2/apple/login`
- Primary path: returning Apple User, warm Apple key cache
- Seeded Apple subjects: `bench-apple-user-<vu>` up to `bench-apple-user-64`
- Apple provider: local stub, not the real Apple network
- Stub delay:
  - `GET /auth/keys`: 80 ms
  - `POST /auth/token`: 300 ms

## Why the Apple provider is stubbed

The primary comparison should isolate backend request-path behavior. Real Apple calls include DNS, TLS, internet routing, and provider-side variability, so they are useful as smoke checks but not as the main before/after evidence.

## Run

Install `k6` first if it is not already available:

```bash
brew install k6
```

Run from the repository root:

```bash
scripts/benchmarks/apple-login/run.sh before quick
scripts/benchmarks/apple-login/run.sh after quick
```

Full evidence runs:

```bash
scripts/benchmarks/apple-login/run.sh before full
scripts/benchmarks/apple-login/run.sh after full
```

The script starts an isolated MySQL container, starts the Apple stub, starts the backend with the `bench` Spring profile, seeds returning Apple Users, runs k6, and writes results.

The script does not run `git checkout`. Measure the checkout you have selected, and use `before` or `after` only as the result label.

## Defaults

- Database container: `ontime-bench-mysql`
- Database: `ontime_bench`
- MySQL port: `127.0.0.1:3307`
- Backend port: `127.0.0.1:18081`
- Apple stub port: `127.0.0.1:18080`
- Quick mode: 1 run per concurrency
- Full mode: 10 runs per concurrency
- Warmup: 2 minutes
- Measurement: 5 minutes
- Concurrency: 1, 10, 20

Each VU uses a distinct returning Apple User. This keeps the benchmark focused on Apple provider round trips instead of same-user active-session token rotation under concurrent login load.

Override example:

```bash
WARMUP_DURATION=10s MEASUREMENT_DURATION=30s SCENARIOS="1" RUNS=1 \
  scripts/benchmarks/apple-login/run.sh before quick
```

## Results

Results are written under:

```text
scripts/benchmarks/apple-login/results/<timestamp>-<label>-<mode>/
```

Each result directory contains:

- `k6-*.json`: k6 measurement summary
- `stub-*.json`: Apple provider hit count during measurement
- `summary.csv`: joined p50/p95/p99, error rate, and provider calls/request
- `backend.log`: backend logs for the run
- `apple-stub.log`: Apple stub logs for the run

Use `docs/performance/apple-login-323.md` for the human-readable comparison table.

## Acceptance Gate

Primary scenario: returning Apple User, warm cache, concurrency 1.

- JWKS network calls/request: `before 1.0 -> after 0.0`
- Apple token exchange calls/request: `before 1.0 -> after 0.0`
- Error rate: `0%`
- p95 latency: `after <= before * 0.70`

Secondary scenarios: returning Apple User, concurrency 10 and 20.

- Error rate: `0%`
- Apple token exchange calls/request: `after 0.0`
- p95 latency: `after <= before`
