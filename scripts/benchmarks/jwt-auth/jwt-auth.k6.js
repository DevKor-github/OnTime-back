import http from "k6/http";
import { check, sleep } from "k6";

const backendUrl = __ENV.BACKEND_URL || "http://127.0.0.1:18082";
const accessToken = __ENV.ACCESS_TOKEN || "";
const vus = Number(__ENV.K6_VUS || 1);
const duration = __ENV.K6_DURATION || "5m";
const resultJson = __ENV.K6_RESULT_JSON || "k6-summary.json";

function metricValue(data, metricName, field, fallback = 0) {
  return data.metrics[metricName]?.values?.[field] ?? data.metrics[metricName]?.[field] ?? fallback;
}

export const options = {
  vus,
  duration,
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
  thresholds: {
    http_req_failed: ["rate==0"],
  },
};

export function setup() {
  if (!accessToken) {
    throw new Error("ACCESS_TOKEN is required");
  }
}

export default function () {
  const response = http.get(`${backendUrl}/users/me/punctuality-score`, {
    headers: {
      authorization: `Bearer ${accessToken}`,
    },
    tags: {
      endpoint: "jwt-auth-punctuality-score",
    },
  });

  check(response, {
    "status is 200": (res) => res.status === 200,
  });

  sleep(0.1);
}

export function handleSummary(data) {
  return {
    [resultJson]: JSON.stringify(data, null, 2),
    stdout: JSON.stringify(
      {
        requests: metricValue(data, "http_reqs", "count", 0),
        failedRate: metricValue(data, "http_req_failed", "rate", 0),
        p50: metricValue(data, "http_req_duration", "p(50)", metricValue(data, "http_req_duration", "med", null)),
        p95: metricValue(data, "http_req_duration", "p(95)", null),
        p99: metricValue(data, "http_req_duration", "p(99)", null),
      },
      null,
      2,
    ) + "\n",
  };
}
