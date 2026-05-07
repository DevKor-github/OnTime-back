# EC2 Deployment

This service deploys to Amazon EC2 through `.github/workflows/deploy.yml`.

## How to Deploy

1. Make sure the EC2 instance has Docker installed and the security group allows inbound traffic for the service port, currently `8080`.
2. Add the required GitHub Actions secrets listed below.
3. Run the `Deploy` workflow manually from GitHub Actions, or push to the `deploy` branch.

The workflow builds the Spring Boot jar, creates deploy-only config files from GitHub Secrets, uploads them to `/home/ubuntu/OnTime-back`, and restarts Docker Compose on the EC2 instance.

## Required EC2 Secrets

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`

## Required Application Secrets

- `SPRING_APPLICATION_NAME`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
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
- `AUTHKEY_743M7R5W3W`
- `SPRING_FLYWAY_URL`
- `SPRING_FLYWAY_USER`
- `SPRING_FLYWAY_PASSWORD`
- `ONTIME_PUSH_FIREBASE_ADMINSDK`

## Optional Secrets

- `SPRING_JPA_DATABASE_PLATFORM` defaults to `org.hibernate.dialect.MySQL8Dialect`.
- `FEATURE_APPLE_LOGIN_ENABLED` defaults to `true`.
- Google and Kakao OAuth provider/registration secrets are included by the workflow when configured.

## Runtime Files on EC2

The deploy workflow writes these files under `/home/ubuntu/OnTime-back`:

- `project.jar`
- `Dockerfile`
- `docker-compose.yml`
- `config/application.properties`
- `secrets/firebase-adminsdk.json`
- `secrets/AuthKey_743M7R5W3W.p8`

Do not commit local `application.properties`, Firebase service account JSON, Apple `.p8` keys, or `.env` files.
