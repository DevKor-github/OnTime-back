import http from "k6/http";
import { check } from "k6";

export const options = {
  scenarios: {
    alarm_window: {
      executor: "constant-vus",
      vus: Number(__ENV.VUS ?? 10),
      duration: __ENV.DURATION ?? "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
  },
};

const baseUrl = __ENV.BASE_URL ?? "http://localhost:8080";
const token = __ENV.ACCESS_TOKEN;

export default function () {
  const url = `${baseUrl}/schedules/alarm-window?startDate=2026-07-01T00:00:00&endDate=2026-07-15T00:00:00`;
  const response = http.get(url, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  check(response, {
    "status is 200": (res) => res.status === 200,
    "returns schedules": (res) => {
      const body = res.json();
      return Array.isArray(body.data) && body.data.length > 0;
    },
  });
}
