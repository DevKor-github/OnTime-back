# Account Deletion Verification Evidence

Issue: #439
Date: 2026-05-10
Status: implementation verified locally; release environment evidence pending

## Scope

This document records backend verification evidence for account deletion. It is
intended to support frontend release QA and privacy review, not to replace
release-environment database, log, analytics, monitoring, or backup checks.

## Verification Run

Command executed from `ontime-back`:

```bash
./gradlew test --rerun-tasks
```

Result: passed.

Relevant test coverage:

- `devkor.ontime_back.service.UserAuthServiceTest`
- `devkor.ontime_back.controller.UserAuthControllerTest`
- `devkor.ontime_back.controller.SocialAuthControllerTest`

## Provider Evidence Matrix

| Provider | Endpoint Called | Environment | Test Account ID | Request Time | Response | Re-login Fails? | Owner | Evidence Link |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Normal | `DELETE /users/me/delete` | Local test | Test fixture | 2026-05-10 | JSON success, message `계정이 성공적으로 삭제되었습니다!` | Not verified locally | Backend repo | `UserAuthControllerTest`, `UserAuthServiceTest` |
| Google | `DELETE /oauth2/google/me` | Local test | Test fixture | 2026-05-10 | JSON success, message `구글 로그인 회원탈퇴 성공` | Not verified locally | Backend repo | `SocialAuthControllerTest`, `UserAuthServiceTest` |
| Apple | `DELETE /oauth2/apple/me` | Local test | Test fixture | 2026-05-10 | JSON success, message `애플 로그인 회원탈퇴 성공` | Not verified locally | Backend repo | `SocialAuthControllerTest`, `UserAuthServiceTest` |

Release QA must replace local-test evidence with staging or
production-equivalent evidence that includes the real environment, account ID,
request timestamp, response, owner, and evidence link.

## Data Deletion And Retention Matrix

| Data Category | Backend Location | Deleted, Anonymized, Retained, or N/A | Retention Duration | Retention Reason | Verification Method | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| User profile fields such as email, name, image, note, role, punctuality fields, and social identity | `user` table | Deleted in local integration test | N/A after deletion | Account deletion hard-delete | `UserAuthService.deleteUser` calls `userRepository.delete(user)`; test asserts deleted user row is absent | Backend repo; release env owner TBD |
| Password or auth credentials for normal accounts | `user.password` | Deleted in local integration test | N/A after deletion | Account deletion hard-delete | User row deletion removes password field | Backend repo; release env owner TBD |
| OAuth provider linkage for Google | `user.social_type`, `user.social_id`, `user.social_login_token`; Google revoke endpoint | Local linkage deleted; provider revoke attempted | Local linkage N/A after deletion; provider-side retention TBD | Local account deletion hard-delete; provider unlink depends on Google revoke result | Controller test verifies deletion continues when Google revoke throws | Backend repo; provider/env owner TBD |
| OAuth provider linkage for Apple | `user.social_type`, `user.social_id`, `user.social_login_token`; Apple revoke endpoint | Local linkage deleted; provider revoke attempted | Local linkage N/A after deletion; provider-side retention TBD | Local account deletion hard-delete; provider unlink depends on Apple revoke result | Controller test verifies deletion continues when Apple revoke throws | Backend repo; provider/env owner TBD |
| Access and refresh tokens | `user.access_token`, `user.refresh_token`, `user_device.session_access_token`, `user_device.session_refresh_token` | Deleted in local integration test | N/A after deletion | Account deletion hard-delete and device cascade | Test asserts user and device rows are absent | Backend repo; release env owner TBD |
| Device records and FCM tokens | `user.firebase_token`, `user_device`, `user_device.firebase_token` | Deleted in local integration test | N/A after deletion | Account deletion hard-delete and device cascade | Test asserts user device row is absent | Backend repo; release env owner TBD |
| Alarm settings and alarm status | `user_alarm_setting`, `user_alarm_status` | Deleted in local integration test | N/A after deletion | Foreign-key cascade from user/device | Test asserts alarm setting and status rows are absent | Backend repo; release env owner TBD |
| Default preparation settings | `preparation_user` | Deleted in local integration test | N/A after deletion | Foreign-key cascade from user | Test asserts preparation user rows are absent | Backend repo; release env owner TBD |
| Schedules | `schedule`, `notification_schedule` | Deleted in local integration test | N/A after deletion | Foreign-key cascade from user and schedule | Test asserts schedule and notification schedule rows are absent | Backend repo; release env owner TBD |
| Schedule preparation steps | `preparation_schedule` | Deleted in local integration test | N/A after deletion | Foreign-key cascade from schedule | Test asserts preparation schedule rows are absent | Backend repo; release env owner TBD |
| Spare time setting | `user.spare_time`, `user_setting` | Deleted in local integration test | N/A after deletion | Account deletion hard-delete and setting cascade | Test asserts user setting row is absent | Backend repo; release env owner TBD |
| General feedback sent through `/feedback` | `feedback` | Deleted in local integration test | N/A after deletion | Foreign-key cascade from user | Test asserts feedback rows are absent | Backend repo; release env owner TBD |
| Account deletion feedback sent in delete request body | `account_deletion_feedback` | Retained with anonymized email hash in local integration test | TBD by privacy/product policy | Product analytics/support reason TBD | `saveAccountDeletionFeedback` stores deleted user ID, social type, SHA-256 normalized email hash, message, and created timestamp without a user foreign key; test asserts retained row and no plaintext email | Backend repo; privacy owner TBD |
| Operational logs, audit logs, crash logs, analytics, or monitoring events | App logs, hosting logs, monitoring/analytics tools | TBD | TBD | TBD | Requires release environment/tooling review | Backend/environment owner |
| Backups or disaster recovery snapshots | Database backups/snapshots | TBD | TBD | TBD | Requires backup policy review | Backend/environment owner |

## Social Token Revocation Decision

Google and Apple deletion endpoints attempt provider token revocation before
local account deletion. If the provider revoke call throws, the controller logs a
warning and still calls `UserAuthService.deleteUser`.

This means a successful social deletion response verifies local OnTime account
deletion, but it does not prove provider-side unlink or revoke success. Provider
unlink must be checked separately with real Google and Apple test accounts in
the release environment.

## Remaining Release/Privacy Checks

- Run deletion in the release verification environment for normal, Google, and
  Apple accounts.
- Attach database/API evidence for every provider row in the matrix.
- Confirm provider-side unlink/re-login behavior for Google and Apple.
- Confirm operational log, crash log, analytics, monitoring, and backup
  retention behavior.
- Record retention duration and reason for account deletion feedback.
- Compare the final evidence with the privacy policy draft before closing #439.
