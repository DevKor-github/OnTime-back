#!/usr/bin/env python3
import csv
import json
import re
import sys
from pathlib import Path


K6_NAME = re.compile(r"k6-(?P<label>[^-]+)-c(?P<concurrency>\d+)-run(?P<run>\d+)\.json")


def metric_value(metrics, name, field, default=0):
    value = metrics.get(name, {})
    values = value.get("values", {})
    return values.get(field, value.get(field, default))


def percentile(metrics, name, key):
    metric = metrics.get(name, {})
    values = metric.get("values", {})
    if key == "p(50)":
        return values.get(key, values.get("med"))
    return values.get(key, metric.get("percentiles", {}).get(key))


def read_json(path):
    with path.open() as file:
        return json.load(file)


def summarize(result_dir):
    rows = []
    for k6_path in sorted(result_dir.glob("k6-*-c*-run*.json")):
        match = K6_NAME.fullmatch(k6_path.name)
        if not match:
            continue

        label = match.group("label")
        concurrency = match.group("concurrency")
        run = match.group("run")
        stub_path = result_dir / f"stub-{label}-c{concurrency}-run{run}.json"
        k6_summary = read_json(k6_path)
        stub_counts = read_json(stub_path) if stub_path.exists() else {}

        metrics = k6_summary.get("metrics", {})
        requests = metric_value(metrics, "apple_login_requests", "count", 0)
        failed_rate = metric_value(metrics, "apple_login_failed", "rate", 0)
        keys = stub_counts.get("keys", 0)
        exchange = stub_counts.get("exchange", 0)

        rows.append(
            {
                "label": label,
                "concurrency": concurrency,
                "run": run,
                "requests": requests,
                "error_rate": failed_rate,
                "p50_ms": percentile(metrics, "apple_login_duration", "p(50)"),
                "p95_ms": percentile(metrics, "apple_login_duration", "p(95)"),
                "p99_ms": percentile(metrics, "apple_login_duration", "p(99)"),
                "jwks_calls": keys,
                "token_exchange_calls": exchange,
                "jwks_calls_per_request": keys / requests if requests else None,
                "token_exchange_calls_per_request": exchange / requests if requests else None,
            }
        )

    return rows


def main():
    if len(sys.argv) != 2:
        print("usage: summarize.py <result-dir>", file=sys.stderr)
        return 2

    result_dir = Path(sys.argv[1]).resolve()
    rows = summarize(result_dir)
    if not rows:
        print(f"no k6 result files found in {result_dir}", file=sys.stderr)
        return 1

    output_path = result_dir / "summary.csv"
    with output_path.open("w", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    print(output_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
