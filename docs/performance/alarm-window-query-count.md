# Alarm Window Query Count and Load Test

`GET /schedules/alarm-window` previously repeated preparation and user-setting reads while mapping each schedule in the response. The optimized path preloads preparation data for the alarm window and avoids fetching the full user graph for each schedule.

## Scenario

- Dataset: 1 user, 25 `NOT_ENDED` schedules in `DEFAULT` preparation mode, 3 default preparation steps.
- Endpoint: `/schedules/alarm-window?startDate=2026-07-01T00:00:00&endDate=2026-07-15T00:00:00`
- HTTP load: k6, 10 virtual users, 30 seconds.
- Runtime: local Spring Boot server backed by H2 in-memory database.

## Results

| Metric | Before | After | Change |
| --- | ---: | ---: | ---: |
| DB prepared statements/request | 52 | 4 | -92.3% |
| HTTP avg latency | 5.86 ms | 2.83 ms | -51.6% |
| HTTP p50 latency | 3.77 ms | 1.99 ms | -47.3% |
| HTTP p95 latency | 14.38 ms | 7.08 ms | -50.7% |
| Throughput | 1,559 req/s | 2,965 req/s | +90.2% |
| HTTP failure rate | 0.00% | 0.00% | no change |

## Commands

```bash
node scripts/benchmarks/alarm-window/generate-data.mjs build/benchmarks/alarm-window
BASE_URL=http://localhost:18080 ACCESS_TOKEN="$(jq -r .accessToken build/benchmarks/alarm-window/token.json)" VUS=10 DURATION=30s k6 run --summary-export build/benchmarks/alarm-window/before-quiet-summary.json scripts/benchmarks/alarm-window/k6.js
BASE_URL=http://localhost:18081 ACCESS_TOKEN="$(jq -r .accessToken build/benchmarks/alarm-window/token.json)" VUS=10 DURATION=30s k6 run --summary-export build/benchmarks/alarm-window/after-summary.json scripts/benchmarks/alarm-window/k6.js
./gradlew test --tests devkor.ontime_back.service.ScheduleAlarmWindowQueryCountTest
```

## Notes

The HTTP benchmark is intended as local comparative evidence, not a production capacity number. During the H2 load run, asynchronous API log inserts can report H2 identity primary-key collisions after responses have already succeeded; k6 still recorded 0 HTTP failures in both before and after runs.
