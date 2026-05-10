# Account Deletion Request Page

Issue: #440

## Public URL

Use the production backend HTTPS host plus the stable path:

```text
https://3.38.172.54.nip.io/account-deletion
```

If the production backend is later moved to a custom domain, keep the same path
and update Google Play Console to the custom-domain URL.

## Hosting Surface

The page is hosted by the Spring Boot backend at `GET /account-deletion`.

It is public and does not require an OnTime access token. The path is explicitly
allowed in both Spring Security route authorization and the custom JWT filter
skip list.

## Request Handling Channel

Outside-app account deletion requests are handled by email:

```text
jjoonleo@gmail.com
```

The authenticated backend API remains available for in-app account deletion.

## Retention Enforcement

Backend-owned database retention is enforced for:

- Optional account deletion feedback: up to 1 year.
- Backend API logs: up to 90 days.

Backup rotation and external monitoring/security log retention must be enforced
by the production infrastructure and logging tools, not only by application code.
