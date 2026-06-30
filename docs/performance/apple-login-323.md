# Apple Login Performance Evidence for Issue #323

## Measurement Contract

This document records before/after evidence for reducing Apple login latency by removing repeated Apple provider round trips from the returning Apple User login path.

Primary benchmark environment:

- Backend: local Spring Boot process with `bench` profile
- Database: isolated Docker MySQL database `ontime_bench`
- Apple provider: local stub
- Returning Apple Users: `social_type=APPLE`, `social_id=bench-apple-user-<vu>`
- Stub delay:
  - `GET /auth/keys`: 80 ms
  - `POST /auth/token`: 300 ms
- Quick run: default harness settings, 2 minutes warmup and 5 minutes measurement, 1 run per scenario
- Full evidence run: bounded local run, 30 seconds warmup and 60 seconds measurement, 3 runs per scenario
- Before checkout: `origin/main` (`ab5fc7f`) plus benchmark harness only (`85b2b18`)
- After checkout: same benchmark harness plus the #323 fix under test

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

The full rows aggregate 3 runs per scenario by summing requests/provider calls and averaging per-run p50/p95 latency.

| mode | scenario | version | runs | requests | error_rate | p50_ms | p95_ms | jwks_calls/request | token_exchange_calls/request |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| quick | c1 returning warm-cache | before | 1 | 590 | 0.000 | 405.6 | 425.5 | 1.000 | 1.000 |
| quick | c1 returning warm-cache | after | 1 | 2,327 | 0.000 | 26.0 | 40.1 | 0.000 | 0.000 |
| quick | c10 returning warm-cache | before | 1 | 5,773 | 0.000 | 417.9 | 435.1 | 1.000 | 1.000 |
| quick | c10 returning warm-cache | after | 1 | 22,366 | 0.000 | 33.7 | 46.5 | 0.000 | 0.000 |
| quick | c20 returning warm-cache | before | 1 | 11,745 | 0.000 | 408.0 | 431.2 | 1.000 | 1.000 |
| quick | c20 returning warm-cache | after | 1 | 47,844 | 0.000 | 20.7 | 46.8 | 0.000 | 0.000 |
| full | c1 returning warm-cache | before | 3 | 346 | 0.000 | 413.6 | 435.8 | 1.000 | 1.000 |
| full | c1 returning warm-cache | after | 3 | 1,407 | 0.000 | 23.1 | 46.5 | 0.000 | 0.000 |
| full | c10 returning warm-cache | before | 3 | 3,492 | 0.000 | 415.1 | 429.1 | 1.000 | 1.000 |
| full | c10 returning warm-cache | after | 3 | 13,425 | 0.000 | 29.3 | 58.5 | 0.000 | 0.000 |
| full | c20 returning warm-cache | before | 3 | 6,829 | 0.000 | 420.4 | 506.9 | 1.000 | 1.000 |
| full | c20 returning warm-cache | after | 3 | 27,824 | 0.000 | 19.9 | 60.4 | 0.000 | 0.000 |

## Result Files

- Quick before: `scripts/benchmarks/apple-login/results/20260628T043649Z-before-quick/summary.csv`
- Quick after: `scripts/benchmarks/apple-login/results/20260628T045808Z-after-quick/summary.csv`
- Full before: `scripts/benchmarks/apple-login/results/20260628T051949Z-before-full/summary.csv`
- Full after: `scripts/benchmarks/apple-login/results/20260628T053451Z-after-full/summary.csv`

## Decision

The #323 fix passes the acceptance gate in both quick and bounded full runs:

- c1 p95 improved from 435.8 ms to 46.5 ms in the full run, an 89.3% reduction.
- Returning Apple User login used 0.0 JWKS network calls/request after warmup.
- Returning Apple User login used 0.0 Apple token exchange calls/request after the fix.
- Error rate stayed at 0% across c1, c10, and c20.

## Notes

- Real Apple network calls are intentionally excluded from the primary benchmark because provider and network variability would obscure backend request-path changes.
- The script labels results as `before` or `after` but does not change git refs. Select the checkout explicitly before running.
- The original single-user concurrency harness caused c10 failures unrelated to #323 because concurrent logins for one **User** raced on single-active-session token rotation. The harness now seeds one returning Apple User per VU so c10/c20 measure Apple provider round trips rather than same-user session contention.
