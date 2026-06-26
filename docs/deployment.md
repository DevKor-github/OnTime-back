# Deployment

This service deploys as an immutable Docker image published to GitHub Container Registry (GHCR). GitHub Actions builds the image, pushes it to GHCR, writes a runtime `.env` file on the target host, and restarts Docker Compose.

Production deploys from `main` with the `production` GitHub Environment. Development deploys from `dev` with the `development` GitHub Environment.

## Secret Policy

Deployment workflows use the same secret names for repository and environment secrets. A value that differs between development and production must be stored as an Environment secret with the same name in each environment. A value that is truly shared by every environment may stay as a repository secret.

GitHub Actions resolves `secrets.NAME` from the selected Environment first, then falls back to repository secrets. Because of that fallback, production-only or database-bearing values must not live only as repository secrets.

Do not add new `DEV_*`, `EC2_*`, or `*_PROD` workflow secret names. The selected GitHub Environment supplies the right value.

The workflows rely on the built-in `GITHUB_TOKEN` for GHCR login. Do not create or use `GHCR_READ_TOKEN` or `GHCR_USERNAME` for deploy pulls.

## Required Secrets

Set environment-specific values in the `development` and `production` GitHub Environments. Keep common values as repository secrets only when the exact same value is safe for both deploys.

Environment-only deployment access:

- `DEPLOY_ENVIRONMENT`
- `REMOTE_HOST`
- `REMOTE_USER`
- `REMOTE_SSH_KEY`
- `DEPLOY_DIR`
- `BACKEND_HTTP_PORT`

Common repository secrets, unless an environment needs an override:

- `BACKEND_MEMORY_LIMIT`
- `BACKEND_CPU_LIMIT`
- `JWT_ACCESS_EXPIRATION`
- `JWT_REFRESH_EXPIRATION`
- `JWT_ACCESS_HEADER`
- `JWT_REFRESH_HEADER`
- `GOOGLE_WEB_CLIENT_ID`
- `GOOGLE_APP_CLIENT_ID`

Environment-only Spring and database:

- `SPRING_APPLICATION_NAME`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Environment-only authentication and integrations:

- `JWT_SECRETKEY`
- `APPLE_CLIENT_ID`
- `APPLE_TEAM_ID`
- `APPLE_LOGIN_KEY`
- `APPLE_PRIVATE_KEY_BASE64`
- `FEATURE_APPLE_LOGIN_ENABLED`
- `FIREBASE_CREDENTIALS_BASE64`

Development-only Environment secrets:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`

Production-only Environment secrets:

- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_AUTHORIZATION_GRANT_TYPE`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_NAME`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_AUTHORIZATION_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_TOKEN_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_USER_INFO_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_USER_NAME_ATTRIBUTE`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_SCOPE`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_REDIRECT_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_AUTHORIZATION_GRANT_TYPE`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_NAME`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_AUTHORIZATION_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_TOKEN_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_USER_INFO_URI`
- `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KAKAO_USER_NAME_ATTRIBUTE`

## Expected Values

Development should normally use:

- `DEPLOY_ENVIRONMENT=development`
- `DEPLOY_DIR=/home/<remote-user>/OnTime-back-dev`
- `BACKEND_HTTP_PORT=8081`
- `SPRING_APPLICATION_NAME=ontime-back-dev`
- `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/ontime_dev?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true`
- `SPRING_DATASOURCE_USERNAME=<development mysql user>`
- `SPRING_DATASOURCE_PASSWORD=<development mysql password>`
- `FEATURE_APPLE_LOGIN_ENABLED=false`

Production must use:

- `DEPLOY_ENVIRONMENT=production`
- `DEPLOY_DIR=/home/ubuntu/OnTime-back`
- `BACKEND_HTTP_PORT=127.0.0.1:8080`
- `SPRING_DATASOURCE_URL` pointing to the private RDS MySQL endpoint and the `ontime_prod` database
- `SPRING_DATASOURCE_USERNAME` not equal to `root`
- `FEATURE_APPLE_LOGIN_ENABLED=true` unless Apple login is intentionally disabled

Common defaults may stay as repository secrets:

- `BACKEND_MEMORY_LIMIT=768m`
- `BACKEND_CPU_LIMIT=1.0`
- `JWT_ACCESS_EXPIRATION=3600000`
- `JWT_REFRESH_EXPIRATION=1209600000`
- `JWT_ACCESS_HEADER=Authorization`
- `JWT_REFRESH_HEADER=Authorization-refresh`

The development workflow fails before restart if `DEPLOY_ENVIRONMENT` is not `development`, if the datasource URL does not point to the Compose `mysql` service, or if the database name is not `ontime_dev`.

The production workflow fails before restart if `DEPLOY_ENVIRONMENT` is not `production`, if the datasource URL does not point to an RDS MySQL endpoint using the `ontime_prod` database, if TLS is disabled, if `allowPublicKeyRetrieval=true` is present, or if EC2 cannot reach RDS on port `3306`.

## Credential Files

Set base64 secrets from ignored local credential files:

```bash
base64 -i ontime-back/src/main/resources/ontime-c63f1-firebase-adminsdk-fbsvc-a043cdc829.json | tr -d '\n' | gh secret set FIREBASE_CREDENTIALS_BASE64 --env production --repo DevKor-github/OnTime-back
```

```bash
base64 -i ontime-back/src/main/resources/key/AuthKey_743M7R5W3W.p8 | tr -d '\n' | gh secret set APPLE_PRIVATE_KEY_BASE64 --env production --repo DevKor-github/OnTime-back
```

Use the same secret names with `--env development` for development-specific credential values.

## Build And Release Flow

Push to `main`, or run `.github/workflows/deploy.yml` manually, to deploy production.

The production workflow:

1. Builds `ontime-back/Dockerfile` from the `ontime-back/` context.
2. Pushes `ghcr.io/devkor-github/ontime-back:<commit-sha>` and `ghcr.io/devkor-github/ontime-back:deploy-latest`.
3. Uploads `docker-compose.yml` and `Caddyfile` to `DEPLOY_DIR`.
4. Writes `DEPLOY_DIR/.env` from `production` Environment secrets.
5. Verifies EC2 can reach private RDS on `3306`.
6. Runs `docker compose pull && docker compose up -d --remove-orphans`.
7. Waits until `ontime-container` is healthy.
8. Configures Caddy and verifies HTTPS readiness for `ontime-back.duckdns.org`.

Push to `dev`, or run `.github/workflows/deploy-dev.yml` manually, to deploy development.

The development workflow:

1. Builds `ontime-back/Dockerfile` from the `ontime-back/` context.
2. Pushes `ghcr.io/devkor-github/ontime-back:dev-<commit-sha>` and `ghcr.io/devkor-github/ontime-back:dev-latest`.
3. Uploads `docker-compose.yml` and `docker-compose.dev.yml` to `DEPLOY_DIR`.
4. Writes `DEPLOY_DIR/.env` from `development` Environment secrets.
5. Runs `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --remove-orphans`.
6. Starts MySQL as a private Docker Compose service with persistent volume `ontime-dev-mysql-data`.
7. Waits until `ontime-dev-container` is healthy.

## Host Prerequisites

Production EC2:

- DNS for `ontime-back.duckdns.org` points to the EC2 public IPv4 address.
- Inbound TCP `80` and `443` are open for Caddy.
- SSH `22` is restricted to trusted admin IPs.
- Public inbound `8080` is not required because the backend binds to `127.0.0.1:8080`.

Development remote PC:

- Ubuntu/Linux host with SSH access from GitHub Actions.
- Docker and the Docker Compose plugin installed.
- Inbound firewall access for `BACKEND_HTTP_PORT`, normally `8081`.
- No public inbound MySQL port is required; MySQL stays inside the Docker network.

## Health Verification

The image exposes Docker health checks against:

```text
/actuator/health/readiness
```

Manual production checks:

```bash
cd /home/ubuntu/OnTime-back
sudo docker compose ps
sudo docker inspect -f '{{.State.Health.Status}}' ontime-container
curl -fsS http://localhost:8080/actuator/health/readiness
curl -fsS https://ontime-back.duckdns.org/actuator/health/readiness
```

Manual development checks:

```bash
cd /home/<remote-user>/OnTime-back-dev
sudo docker compose -f docker-compose.yml -f docker-compose.dev.yml ps
sudo docker inspect -f '{{.State.Health.Status}}' ontime-dev-container
sudo docker logs --tail=200 ontime-dev-container
curl -fsS http://<remote-pc-host>:8081/actuator/health/readiness
```
