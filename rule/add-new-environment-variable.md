# How To Add A New Environment Variable

Use this checklist whenever the backend needs a new configurable value.

## 1. Choose the property names

- Environment variable names use `UPPER_SNAKE_CASE`, for example `FEATURE_APPLE_LOGIN_ENABLED`.
- Spring property names use lowercase dot notation, for example `feature.apple-login.enabled`.
- Secret values must never be committed. Commit only placeholders, examples, or property references.

## 2. Add the Spring property reference

Update `ontime-back/src/main/resources/application.example.properties`.

Use this shape:

```properties
some.spring.property=${SOME_ENV_VAR:non-secret-default}
```

For required secrets, prefer an empty or obvious placeholder default:

```properties
external.api.secret=${EXTERNAL_API_SECRET:replace-me}
```

Do not put a real token, password, private key, service account JSON, or production URL with credentials in this file.

## 3. Add the local example variable

Update `ontime-back/.env.example`.

Example:

```dotenv
SOME_ENV_VAR=example-value
EXTERNAL_API_SECRET=replace-me
```

The real local `.env` is ignored by Git. Developers should copy the example key into their own local `.env` and fill in the private value there.

## 4. Wire the value into GitHub Actions

If CI or deploy needs the value, update the workflow that creates `application.properties`.

- PR/test workflow: `.github/workflows/test.yml`
- Deploy workflow: `.github/workflows/deploy.yml`

For non-secret test-only values, a literal value is acceptable:

```yaml
echo "feature.example.enabled=true" >> ontime-back/src/main/resources/application.properties
```

For secrets or production values, use GitHub Actions Secrets:

```yaml
echo "external.api.secret=${{ secrets.EXTERNAL_API_SECRET }}" >> deploy-secrets/application.properties
```

Then add the corresponding repository or environment secret in GitHub Settings.

## 5. Keep files with real secrets out of Git

Before staging, check that real local files are ignored:

```bash
git check-ignore -v ontime-back/.env ontime-back/src/main/resources/application.properties
```

Also check the staged diff:

```bash
git diff --cached
```

If a real secret appears in staged output, stop and unstage it before committing.

## 6. Document deployment-only setup

If the variable requires an external file or server-side setup, document the remaining owner/admin work under `handoff/`.

Examples:

- a new key file that must be placed under `/etc/ontime`
- a new GitHub Actions Secret that must be created
- a credential that must be rotated
- a cloud console setting that must be configured

## 7. Verify

Run at least:

```bash
cd ontime-back
./gradlew compileJava
```

Run `./gradlew test` when the required local services, especially MySQL, are available.
