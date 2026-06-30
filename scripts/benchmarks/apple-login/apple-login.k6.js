import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const backendUrl = __ENV.BACKEND_URL || "http://127.0.0.1:18081";
const stubUrl = __ENV.APPLE_STUB_URL || "http://127.0.0.1:18080";
const vus = Number(__ENV.K6_VUS || 1);
const duration = __ENV.K6_DURATION || "5m";
const resultJson = __ENV.K6_RESULT_JSON || "k6-summary.json";

const loginRequests = new Counter("apple_login_requests");
const loginFailed = new Rate("apple_login_failed");
const loginDuration = new Trend("apple_login_duration", true);

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ["rate==0"],
  },
};

export function setup() {
  const payloads = [];
  for (let index = 1; index <= vus; index += 1) {
    const subject = `bench-apple-user-${index}`;
    const response = http.get(`${stubUrl}/fixture/apple-login-payload?subject=${subject}`);
    if (response.status !== 200) {
      throw new Error(`fixture request failed for ${subject} with status ${response.status}`);
    }
    payloads.push(response.json());
  }
  return payloads;
}

export default function (payloads) {
  const payload = payloads[(__VU - 1) % payloads.length];
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
  loginRequests.add(1);
  loginFailed.add(response.status !== 200);
  loginDuration.add(response.timings.duration);

  sleep(0.1);
}

export function handleSummary(data) {
  const requestValues = data.metrics.apple_login_requests?.values || {};
  const failedValues = data.metrics.apple_login_failed?.values || {};
  const durationValues = data.metrics.apple_login_duration?.values || {};

  return {
    [resultJson]: JSON.stringify(data, null, 2),
    stdout: JSON.stringify(
      {
        requests: requestValues.count || 0,
        failedRate: failedValues.rate || 0,
        p50: durationValues["p(50)"] || durationValues.med || null,
        p95: durationValues["p(95)"] || null,
        p99: durationValues["p(99)"] || null,
      },
      null,
      2,
    ) + "\n",
  };
}
