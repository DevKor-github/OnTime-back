# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Spring Boot backend in `ontime-back/`. Main Java code lives under `ontime-back/src/main/java/devkor/ontime_back/`, organized by role: `controller`, `service`, `repository`, `entity`, `dto`, `config`, `scheduler`, `response`, and `global` authentication/JWT support. Tests mirror the main package under `ontime-back/src/test/java/devkor/ontime_back/`. Database migrations are Flyway SQL files in `ontime-back/src/main/resources/db/migration/` and must keep the `V#__description.sql` naming pattern.

## Build, Test, and Development Commands

Run commands from `ontime-back/` unless noted.

- `./gradlew test`: runs JUnit 5 tests.
- `./gradlew build`: compiles, tests, and packages the service.
- `./gradlew bootRun`: starts the Spring Boot app locally.
- `docker compose up --build`: builds and runs the backend container defined in `docker-compose.yml`.

The project uses Java 17 via the Gradle toolchain. CI also runs `./gradlew test` against MySQL 8 with Flyway enabled.

## Coding Style & Naming Conventions

Use Java conventions with 4-space indentation. Keep class names in `PascalCase`, methods and fields in `camelCase`, constants in `UPPER_SNAKE_CASE`, and packages lowercase. Follow existing suffixes such as `*Controller`, `*Service`, `*Repository`, `*Dto`, and `*Response`. Prefer constructor injection and Lombok patterns already used in the codebase. Keep API response shapes consistent with `ApiResponseForm` and centralized errors in `response/`.

## Testing Guidelines

Tests use Spring Boot Test, Spring Security Test, JUnit 5, and Mockito-style unit testing where appropriate. Name test classes after the unit under test, for example `ScheduleServiceTest` or `UserControllerTest`. Add service tests for business rules and controller tests for request/response and security behavior. Run `./gradlew test` before opening a PR; migration changes should be validated against MySQL because CI checks Flyway history.

## Commit & Pull Request Guidelines

Recent commits use short prefixes such as `fix:`, `refactor:`, and `[Deploy]`. Prefer imperative, scoped messages, for example `fix: validate schedule owner before update`. PRs should target `main`, describe the change, list test results, and link related issues when available. Include screenshots only for Swagger/API documentation or externally visible behavior changes. Do not commit generated secrets, Firebase service account JSON, Apple `.p8` keys, or local `application.properties`.

## Security & Configuration Tips

Runtime configuration is provided through `application.properties` in `ontime-back/src/main/resources/` during CI/deploy. Keep credentials in GitHub Secrets or local environment-specific files. When adding OAuth, JWT, Firebase, or database settings, document the property names without checking in secret values.
