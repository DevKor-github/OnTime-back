# EC2 Deployment

This service deploys to Amazon EC2 through `.github/workflows/deploy.yml`.

## How to Deploy

1. Make sure the EC2 instance has Docker installed.
2. Point `ontime-back.duckdns.org` to the EC2 public IPv4 address.
3. Allow inbound security group traffic on TCP `80` and `443`.
4. Add the required GitHub Actions secrets listed below.
5. Run the `Deploy` workflow manually from GitHub Actions, or push to the `main` branch.

The workflow builds a Docker image, pushes it to GHCR, uploads `docker-compose.yml` and `Caddyfile` to `/home/ubuntu/OnTime-back`, writes a production `.env` from GitHub Secrets, verifies private RDS connectivity, restarts Docker Compose, and configures Caddy HTTPS on the EC2 instance.

## Required EC2 Secrets

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`

## Required Application Secrets

- `SPRING_APPLICATION_NAME`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRETKEY`
- `JWT_ACCESS_EXPIRATION`
- `JWT_REFRESH_EXPIRATION`
- `JWT_ACCESS_HEADER`
- `JWT_REFRESH_HEADER`
- `GOOGLE_WEB_CLIENT_ID`
- `GOOGLE_APP_CLIENT_ID`
- `APPLE_CLIENT_ID`
- `APPLE_LOGIN_KEY`
- `APPLE_TEAM_ID`
- `APPLE_PRIVATE_KEY_BASE64`
- `FIREBASE_CREDENTIALS_BASE64`

## Optional Secrets

- `BACKEND_HTTP_PORT` defaults to `127.0.0.1:8080`. If this secret exists, it must be exactly `127.0.0.1:8080`.
- `BACKEND_MEMORY_LIMIT` defaults to `768m`; use `640m` if the EC2 instance is memory constrained.
- `BACKEND_CPU_LIMIT` defaults to `1.0`.
- `FEATURE_APPLE_LOGIN_ENABLED` defaults to `true`.
- Google and Kakao OAuth provider/registration secrets are included by the workflow when configured.

## Runtime Files on EC2

The deploy workflow writes these files under `/home/ubuntu/OnTime-back`:

- `docker-compose.yml`
- `Caddyfile`
- `.env`

Caddy is installed as a systemd service on the host and serves:

```text
https://ontime-back.duckdns.org
```

It proxies to the Spring Boot container through `http://127.0.0.1:8080`. After HTTPS is verified, remove public inbound `8080` from the EC2 security group.

Production uses the private RDS instance:

```text
ontime-prod.cpoeguokwaq5.ap-northeast-2.rds.amazonaws.com:3306/ontime_prod
```

Do not commit local `application.properties`, Firebase service account JSON, Apple `.p8` keys, or `.env` files.
