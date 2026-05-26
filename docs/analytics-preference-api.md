# Analytics Preference API

Issue: #318

## Summary

The analytics preference API stores whether the signed-in account allows optional
Product Usage Events. The preference is account-scoped, not device-scoped.

This API does not define Firebase event names, frontend instrumentation, local
pre-login preference storage, UI copy, marketing analytics, personalization, or
Firebase Remote Config behavior.

## Default And Release Gate

Backend default is controlled by:

```properties
analytics.preference.default-enabled=${ANALYTICS_PREFERENCE_DEFAULT_ENABLED:false}
```

The default remains `false` until the privacy policy and Google Play Data Safety
updates are approved for the Firebase Analytics release. After approval, deploy
owners may set `ANALYTICS_PREFERENCE_DEFAULT_ENABLED=true`.

Rows created before the default is flipped are marked as not user-overridden.
When the service reads a non-overridden row, it may align that row to the current
deploy default. Once a user explicitly updates the preference, the row is marked
as user-overridden and future default flips do not change that choice.

## Get Analytics Preference

```http
GET /users/me/analytics-preference
Authorization: Bearer <access token>
```

Successful response:

```json
{
  "status": "success",
  "code": 200,
  "message": "OK",
  "data": {
    "enabled": false,
    "updatedAt": "2026-05-26T12:00:00Z"
  }
}
```

## Update Analytics Preference

```http
PUT /users/me/analytics-preference
Authorization: Bearer <access token>
Content-Type: application/json

{
  "enabled": true
}
```

Successful response:

```json
{
  "status": "success",
  "code": 200,
  "message": "OK",
  "data": {
    "enabled": true,
    "updatedAt": "2026-05-26T12:00:05Z"
  }
}
```

`enabled` is required and must be a JSON boolean. Missing, null, non-boolean, or
unknown fields are rejected with the existing validation-style `400` response.

## Persistence And Deletion

The preference is stored in `user_analytics_preference` with a unique foreign key
to `user(user_id)`, `enabled`, `updated_at`, and the internal
`user_overridden` flag.

On account deletion, the local analytics preference row is deleted by foreign-key
cascade. Future user-linked Product Usage Events stop when the account
preference is disabled or the account is deleted. Historical analytics may be
retained only in aggregate or de-identified form, subject to the approved privacy
policy and Firebase/analytics project configuration.
