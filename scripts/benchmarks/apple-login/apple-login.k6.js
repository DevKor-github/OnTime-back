import http from "k6/http";
import { check, sleep } from "k6";

const backendUrl = __ENV.BACKEND_URL || "http://127.0.0.1:18081";
const stubUrl = __ENV.APPLE_STUB_URL || "http://127.0.0.1:18080";
const vus = Number(__ENV.K6_VUS || 1);
const duration = __ENV.K6_DURATION || "5m";
const resultJson = __ENV.K6_RESULT_JSON || "k6-summary.json";

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ["rate==0"],
  },
};

export function setup() {
  const response = http.get(`${stubUrl}/fixture/apple-login-payload`);
  if (response.status !== 200) {
    throw new Error(`fixture request failed with status ${response.status}`);
  }
  return response.json();
}

export default function (payload) {
  const response = http.post(
    `${backendUrl}/oauth2/apple/login`,
    JSON.stringify(payload),
    {
      headers: {
        "content-type": "application/json",
      },
      tags: {
        endpoint: "apple-login",
      },
    },
  );

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
        requests: data.metrics.http_reqs?.count || 0,
        failedRate: data.metrics.http_req_failed?.rate || 0,
        p50: data.metrics.http_req_duration?.percentiles?.["p(50)"] || null,
        p95: data.metrics.http_req_duration?.percentiles?.["p(95)"] || null,
        p99: data.metrics.http_req_duration?.percentiles?.["p(99)"] || null,
      },
      null,
      2,
    ) + "\n",
  };
}
