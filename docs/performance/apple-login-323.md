# Apple Login Performance Evidence for Issue #323

## Measurement Contract

This document records before/after evidence for reducing Apple login latency by removing repeated Apple provider round trips from the returning Apple User login path.

Primary benchmark environment:

- Backend: local Spring Boot process with `bench` profile
- Database: isolated Docker MySQL database `ontime_bench`
- Apple provider: local stub
- Returning Apple User: `social_type=APPLE`, `social_id=bench-apple-user`
- Stub delay:
  - `GET /auth/keys`: 80 ms
  - `POST /auth/token`: 300 ms
- Warmup: 2 minutes
- Measurement: 5 minutes
- Full run count: 10 per scenario

The benchmark harness lives in `scripts/benchmarks/apple-login/`.

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

## Results

Populate this table from `scripts/benchmarks/apple-login/results/*/summary.csv` after running both before and after measurements.

| scenario | version | runs | requests | error_rate | p50_ms | p95_ms | p99_ms | jwks_calls/request | token_exchange_calls/request |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| c1 returning warm-cache | before | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD |
| c1 returning warm-cache | after | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD |
| c10 returning warm-cache | before | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD |
| c10 returning warm-cache | after | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD |
| c20 returning warm-cache | before | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD |
| c20 returning warm-cache | after | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD |

## Notes

- Real Apple network calls are intentionally excluded from the primary benchmark because provider and network variability would obscure backend request-path changes.
- The script labels results as `before` or `after` but does not change git refs. Select the checkout explicitly before running.
- Quick mode is for development feedback. Full mode is the acceptance evidence.
