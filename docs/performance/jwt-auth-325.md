# JWT Authentication Performance Evidence for Issue #325

## Measurement Contract

This document records before/after evidence for removing duplicate `User` reads from the protected API JWT authentication path.

Primary benchmark environment:

- Backend: local Spring Boot process with `bench` profile
- Database: isolated Docker MySQL database `ontime_jwt_bench`
- Protected endpoint: `GET /users/me/punctuality-score`
- Benchmark User: created through `POST /sign-up`
- Warmup: 20 protected requests
- Measurement: 5 minutes
- Full run count: 10 per scenario

The benchmark harness lives in `scripts/benchmarks/jwt-auth/`.

Recorded quick evidence run:

- before: `scripts/benchmarks/jwt-auth/results/20260628T042635Z-before-quick/summary.csv`
- after: `scripts/benchmarks/jwt-auth/results/20260628T044205Z-after-quick/summary.csv`
- mode: `quick`
- runs: 1 per scenario

## Acceptance Gate

Primary correctness gate:

- Authentication `User` lookup count: `before 2 -> after 1`
- `GET /users/me/punctuality-score` whole-request SQL budget: `after <= 5`
- Invalid, expired, and replaced access token behavior remains compatible
- Refresh token rotation behavior remains compatible
- `./gradlew check` passes

Benchmark evidence:

- Error rate: `0%`
- p95 latency: `after <= before`
- Whole-request SQL budget sample: `after <= before - 1`

## Results

Populate this table from `scripts/benchmarks/jwt-auth/results/*/summary.csv` and `sql-budget-*.txt` after running both before and after measurements.

| scenario | version | runs | requests | error_rate | p50_ms | p95_ms | p99_ms | whole_request_sql_sample |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| c1 protected request | before | 1 | 2273 | 0 | 28.190 | 46.770 | 53.554 | 7 |
| c1 protected request | after | 1 | 2404 | 0 | 21.664 | 38.093 | 48.579 | 5 |
| c10 protected request | before | 1 | 21804 | 0 | 35.879 | 52.873 | 62.045 | 7 |
| c10 protected request | after | 1 | 23409 | 0 | 26.412 | 40.129 | 48.728 | 5 |
| c20 protected request | before | 1 | 47625 | 0 | 23.957 | 36.560 | 72.784 | 7 |
| c20 protected request | after | 1 | 49550 | 0 | 19.597 | 29.620 | 39.054 | 5 |

## Quick Run Delta

| scenario | p95_delta_ms | p95_delta_pct | sql_sample_delta |
| --- | ---: | ---: | ---: |
| c1 protected request | -8.678 | -18.6% | -2 |
| c10 protected request | -12.744 | -24.1% | -2 |
| c20 protected request | -6.940 | -19.0% | -2 |

## Notes

- Query-count correctness is intentionally enforced by automated tests because latency is sensitive to JVM warmup, local machine load, and database state.
- The protected endpoint includes existing API logging work, so the whole-request SQL budget is higher than the authentication-only `User` lookup count.
- The script labels results as `before` or `after` but does not change git refs. Select the checkout explicitly before running.
- Quick mode is for development feedback. Full mode is the acceptance evidence.
