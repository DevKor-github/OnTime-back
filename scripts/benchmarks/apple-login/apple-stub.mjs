import http from "node:http";
import { createSign, generateKeyPairSync } from "node:crypto";

const port = Number(process.env.APPLE_STUB_PORT || 18080);
const clientId = process.env.APPLE_CLIENT_ID || "club.devkor.ontime.bench";
const subject = process.env.BENCH_APPLE_SUB || "bench-apple-user";
const fullName = process.env.BENCH_APPLE_FULL_NAME || "Bench User";
const email = process.env.BENCH_APPLE_EMAIL || "bench.apple@example.com";
const keyDelayMs = Number(process.env.APPLE_KEYS_DELAY_MS || 80);
const exchangeDelayMs = Number(process.env.APPLE_EXCHANGE_DELAY_MS || 300);
const kid = "bench-rsa-key";

const { publicKey, privateKey } = generateKeyPairSync("rsa", {
  modulusLength: 2048,
});

const publicJwk = publicKey.export({ format: "jwk" });
let counts = {
  keys: 0,
  exchange: 0,
};

function base64url(input) {
  return Buffer.from(input)
    .toString("base64")
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replaceAll("=", "");
}

function jsonResponse(response, statusCode, body) {
  response.writeHead(statusCode, {
    "content-type": "application/json",
    "cache-control": "no-store",
  });
  response.end(JSON.stringify(body));
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function readBody(request) {
  return new Promise((resolve, reject) => {
    let body = "";
    request.setEncoding("utf8");
    request.on("data", (chunk) => {
      body += chunk;
    });
    request.on("end", () => resolve(body));
    request.on("error", reject);
  });
}

function signedIdentity() {
  const nowSeconds = Math.floor(Date.now() / 1000);
  const header = {
    alg: "RS256",
    kid,
    typ: "JWT",
  };
  const payload = {
    iss: "https://appleid.apple.com",
    aud: clientId,
    exp: nowSeconds + 3600,
    iat: nowSeconds,
    sub: subject,
    email,
  };
  const signingInput = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(payload))}`;
  const signature = createSign("RSA-SHA256")
    .update(signingInput)
    .sign(privateKey);
  return `${signingInput}.${base64url(signature)}`;
}

const server = http.createServer(async (request, response) => {
  try {
    const url = new URL(request.url, `http://${request.headers.host}`);

    if (request.method === "GET" && url.pathname === "/auth/keys") {
      counts.keys += 1;
      await delay(keyDelayMs);
      jsonResponse(response, 200, {
        keys: [
          {
            kty: "RSA",
            kid,
            alg: "RS256",
            n: publicJwk.n,
            e: publicJwk.e,
          },
        ],
      });
      return;
    }

    if (request.method === "POST" && url.pathname === "/auth/token") {
      counts.exchange += 1;
      await readBody(request);
      await delay(exchangeDelayMs);
      jsonResponse(response, 200, {
        access_token: "bench-apple-access",
        refresh_token: "bench-apple-refresh",
        id_token: signedIdentity(),
        token_type: "Bearer",
        expires_in: 3600,
      });
      return;
    }

    if (request.method === "GET" && url.pathname === "/fixture/apple-login-payload") {
      jsonResponse(response, 200, {
        idToken: signedIdentity(),
        authCode: "bench-auth-code",
        fullName,
        email,
      });
      return;
    }

    if (request.method === "POST" && url.pathname === "/__reset") {
      counts = { keys: 0, exchange: 0 };
      jsonResponse(response, 200, counts);
      return;
    }

    if (request.method === "GET" && url.pathname === "/__counts") {
      jsonResponse(response, 200, counts);
      return;
    }

    jsonResponse(response, 404, { error: "not_found" });
  } catch (error) {
    jsonResponse(response, 500, { error: error.message });
  }
});

server.listen(port, "127.0.0.1", () => {
  console.log(`Apple benchmark stub listening on http://127.0.0.1:${port}`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    server.close(() => process.exit(0));
  });
}
