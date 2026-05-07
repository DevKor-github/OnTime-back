# Logging Redaction Policy

Production logs must only contain operational metadata needed to debug routing,
ownership, status, and latency. Request payloads are not safe log data.

## Request Logs

Controller request logging is centralized in `LoggingAspect` and
`RequestLogPolicy`. Request logs must include only:

- request ID
- route
- method
- actor identifier
- client IP
- response status
- timing in milliseconds

The request logger must not inspect or render `@RequestBody` arguments.

## Sensitive Fields

Never log values or raw key/value payloads for credentials, OAuth material,
profile text, notes, or request bodies. Sensitive key names include:

- `authorization`
- `firebaseToken`
- `password`
- `secret`
- `token`

When field-level logging is genuinely needed, add the field to
`RequestLogPolicy`'s allowlist and keep the log statement metadata-only.

## Regression Guard

`SensitiveLoggingPolicyTest` scans application source for sensitive key names in
logger calls, request-body logging in `LoggingAspect`, and DTO-generated
`toString` methods. Any future exception must update this policy and the
central allowlist in the same change.
