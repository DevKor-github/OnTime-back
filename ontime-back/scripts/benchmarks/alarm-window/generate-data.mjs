import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const outDir = process.argv[2] ?? "build/benchmarks/alarm-window";
const scheduleCount = Number(process.env.SCHEDULE_COUNT ?? 25);
const secret = process.env.JWT_SECRET_KEY ?? "test_secret_key_for_ontime_back_application_tests_1234567890";
const userId = 1;
const email = "alarm-window-load@example.com";

fs.mkdirSync(outDir, { recursive: true });

function base64url(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function signJwt(payload) {
  const header = { alg: "HS512", typ: "JWT" };
  const encodedHeader = base64url(JSON.stringify(header));
  const encodedPayload = base64url(JSON.stringify(payload));
  const signature = crypto
    .createHmac("sha512", secret)
    .update(`${encodedHeader}.${encodedPayload}`)
    .digest("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
  return `${encodedHeader}.${encodedPayload}.${signature}`;
}

function sqlString(value) {
  return String(value).replaceAll("'", "''");
}

function uuid(index) {
  return `00000000-0000-4000-8000-${String(index).padStart(12, "0")}`;
}

const nowSeconds = Math.floor(Date.now() / 1000);
const accessToken = signJwt({
  sub: "AccessToken",
  exp: nowSeconds + 60 * 60,
  jti: crypto.randomUUID(),
  email,
  userId,
});

const statements = [];
statements.push(
  `INSERT INTO user (user_id, email, password, name, spare_time, note, punctuality_score, schedule_count_after_reset, lateness_count_after_reset, role, social_type, social_id, access_token, refresh_token, firebase_token, social_login_token, image_url) VALUES (${userId}, '${email}', 'password', 'loaduser', 10, NULL, -1, 0, 0, 'USER', NULL, NULL, '${sqlString(accessToken)}', NULL, NULL, NULL, NULL);`
);
statements.push(
  "INSERT INTO place (place_id, place_name) VALUES ('00000000-0000-4000-8000-000000000001', 'Load Test Place');"
);
statements.push(
  "INSERT INTO preparation_user (preparation_user_id, user_id, preparation_name, preparation_time, order_index, next_preparation_id) VALUES ('00000000-0000-4000-8000-100000000003', 1, 'Pack bag', 5, 2, NULL);"
);
statements.push(
  "INSERT INTO preparation_user (preparation_user_id, user_id, preparation_name, preparation_time, order_index, next_preparation_id) VALUES ('00000000-0000-4000-8000-100000000002', 1, 'Get dressed', 10, 1, '00000000-0000-4000-8000-100000000003');"
);
statements.push(
  "INSERT INTO preparation_user (preparation_user_id, user_id, preparation_name, preparation_time, order_index, next_preparation_id) VALUES ('00000000-0000-4000-8000-100000000001', 1, 'Wash up', 10, 0, '00000000-0000-4000-8000-100000000002');"
);
statements.push(
  "INSERT INTO user_alarm_setting (user_alarm_setting_id, user_id, alarms_enabled, default_alarm_offset_minutes, updated_at) VALUES (1, 1, TRUE, 5, '2026-07-01T00:00:00Z');"
);

for (let i = 0; i < scheduleCount; i += 1) {
  const day = String(1 + Math.floor(i / 8)).padStart(2, "0");
  const hour = String(8 + (i % 8)).padStart(2, "0");
  statements.push(
    `INSERT INTO schedule (schedule_id, user_id, place_id, schedule_name, move_time, schedule_time, is_change, is_started, started_at, finished_at, done_status, preparation_mode, preparation_template_id, schedule_spare_time, lateness_time, schedule_note) VALUES ('${uuid(200000000000 + i)}', 1, '00000000-0000-4000-8000-000000000001', 'Load schedule ${i}', 20, '2026-07-${day}T${hour}:00:00', FALSE, FALSE, NULL, NULL, 'NOT_ENDED', 'DEFAULT', NULL, NULL, -1, NULL);`
  );
}

const dataSql = `${statements.join("\n")}\n`;
const tokenJson = JSON.stringify({ accessToken, userId, scheduleCount }, null, 2);

fs.writeFileSync(path.join(outDir, "data.sql"), dataSql);
fs.writeFileSync(path.join(outDir, "token.json"), `${tokenJson}\n`);

console.log(`Wrote ${path.join(outDir, "data.sql")}`);
console.log(`Wrote ${path.join(outDir, "token.json")}`);
