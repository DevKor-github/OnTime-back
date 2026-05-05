# Account Deletion API

Frontend integration guide for deleting an OnTime account with optional withdrawal feedback.

## Summary

Account deletion hard-deletes the user from OnTime. The request can optionally include feedback. If feedback is provided, the backend stores it separately from the `User` table so it remains available after the account is deleted.

For Google and Apple social accounts, the backend first tries to revoke the social login token, then deletes the local OnTime account.

## Authentication

All endpoints require the current OnTime access token.

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

## Request Body

The request body is optional for every deletion endpoint.

```json
{
  "feedbackId": "d784cde3-9ff9-4054-872a-500bbcc2198a",
  "message": "I do not use the app anymore."
}
```

Fields:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `feedbackId` | UUID string | No | Client-generated ID. If omitted, the backend generates one. |
| `message` | string | No | If missing, blank, or only whitespace, feedback is not saved and deletion still proceeds. |

## General Account Deletion

Use this endpoint for normal OnTime account deletion.

```http
DELETE /users/me/delete
```

Example without feedback:

```http
DELETE /users/me/delete
Authorization: Bearer {accessToken}
Content-Type: application/json

{}
```

Example with feedback:

```http
DELETE /users/me/delete
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "feedbackId": "d784cde3-9ff9-4054-872a-500bbcc2198a",
  "message": "The notifications were not useful for me."
}
```

Success response:

```json
{
  "status": "success",
  "code": "200",
  "message": "계정이 성공적으로 삭제되었습니다!",
  "data": null
}
```

## Google Account Deletion

Use this endpoint when the current account is linked through Google login.

```http
DELETE /oauth2/google/me
```

Example:

```http
DELETE /oauth2/google/me
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "message": "I am switching to another calendar app."
}
```

Success response:

```text
구글 로그인 회원탈퇴 성공
```

Behavior:

- Revokes the stored Google OAuth token for OnTime.
- Deletes the local OnTime account.
- Saves optional feedback if `message` is nonblank.
- Does not delete the user's actual Google account.

Expected frontend result after successful revoke:

- If the user signs up or logs in with the same Google account again, Google may show an "OnTime에 다시 로그인하는 중입니다" confirmation screen.
- The Google account can still be preselected because the user is still signed in to Google on the device/browser.

Important caveat:

- The Google unlink only works if the backend has a valid Google refresh/access token saved in `socialLoginToken`.
- If the client never provided a real Google refresh token, Google revoke may fail and the endpoint may return an error before local deletion.

## Apple Account Deletion

Use this endpoint when the current account is linked through Apple login.

```http
DELETE /oauth2/apple/me
```

Example:

```http
DELETE /oauth2/apple/me
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "feedbackId": "85fc54e0-e6c7-4c7e-9312-7784a52bf120",
  "message": "I want to restart with a fresh account."
}
```

Success response:

```text
애플 로그인 회원탈퇴 성공
```

Behavior:

- Revokes the stored Apple OAuth token for OnTime.
- Deletes the local OnTime account.
- Saves optional feedback if `message` is nonblank.
- Does not delete the user's Apple ID.

## What Gets Stored For Feedback

When feedback is provided, the backend stores:

| Stored Field | Notes |
| --- | --- |
| `feedbackId` | Client-provided UUID or backend-generated UUID |
| `deletedUserId` | Previous OnTime user ID |
| `socialType` | `GOOGLE`, `APPLE`, or null for non-social accounts |
| `emailHash` | SHA-256 hash of the normalized email, not plaintext email |
| `message` | User feedback text |
| `createdAt` | Server timestamp |

Feedback is not linked by foreign key to the deleted user.

## Frontend Recommendations

- Treat feedback as optional. Do not block deletion if the user skips it.
- Generate a UUID for `feedbackId` if convenient, but it is safe to omit.
- Use `/oauth2/google/me` for Google-linked accounts if the product requirement is to unlink Google access.
- Use `/oauth2/apple/me` for Apple-linked accounts if the product requirement is to unlink Apple access.
- Use `/users/me/delete` for normal local deletion.
- After a successful deletion response, clear local auth state and navigate to the logged-out screen.
- Do not retry automatically on social revoke errors without showing the user, because the local account may not have been deleted.
