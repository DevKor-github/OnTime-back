# Schedule Start API

Frontend integration guide for server-authoritative preparation start state.

## Summary

The backend now uses `startedAt` as the source of truth for whether preparation has started. Flutter should stop treating client-side `isStarted` as authoritative.

Preparation becomes frozen when the user starts a schedule:

- Default preparation is a mutable template.
- Started schedule preparation is a schedule-specific snapshot.
- After `startedAt` is set, schedule preparation reads come from the frozen snapshot, not from the user's default preparation.

## Authentication

All endpoints require the current OnTime access token.

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

## Schedule Response Field

Schedule responses now include nullable `startedAt`.

```json
{
  "scheduleId": "3fa85f64-5717-4562-b3fc-2c963f66afe5",
  "place": {
    "placeId": "70d460da-6a82-4c57-a285-567cdeda5670",
    "placeName": "Home"
  },
  "scheduleName": "Party",
  "moveTime": 20,
  "scheduleTime": "2026-05-13T19:30:00",
  "scheduleSpareTime": 20,
  "scheduleNote": "Write a message.",
  "latenessTime": -1,
  "doneStatus": "NOT_ENDED",
  "startedAt": "2026-05-13T10:15:30Z",
  "finishedAt": null
}
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `startedAt` | ISO-8601 UTC datetime or `null` | `null` means preparation has not explicitly started. Non-null means the schedule is locked for editing. |
| `finishedAt` | ISO-8601 UTC datetime or `null` | Non-null means the schedule was explicitly finished by the finish endpoint. |
| `doneStatus` | enum | `NOT_ENDED`, `NORMAL`, `LATE`, or `ABNORMAL`. Finished schedules cannot be edited or deleted. |
| `latenessTime` | integer or `null` | Completion result. `-1` is legacy/unended data; new finish calls use `0` for normal or positive minutes for late. |

Frontend rule:

```text
canEditSchedule = doneStatus == "NOT_ENDED" && startedAt == null
canDeleteSchedule = doneStatus == "NOT_ENDED"
```

## Start Preparation

Call this endpoint when the user taps "Start preparation".

```http
POST /schedules/{scheduleId}/start
```

Request body: none.

Behavior:

- If the schedule has not started, backend sets `startedAt` to server time.
- If the schedule still uses default preparation, backend copies the current default preparation into schedule-specific preparation rows.
- If the schedule already started, backend returns success without changing `startedAt` or replacing the frozen preparation snapshot.
- If the schedule is finished, backend returns `409 SCHEDULE_ALREADY_FINISHED`.

Success response:

```json
{
  "status": "success",
  "code": 200,
  "message": "OK",
  "data": {
    "schedule": {
      "scheduleId": "3fa85f64-5717-4562-b3fc-2c963f66afe5",
      "place": {
        "placeId": "70d460da-6a82-4c57-a285-567cdeda5670",
        "placeName": "Home"
      },
      "scheduleName": "Party",
      "moveTime": 20,
      "scheduleTime": "2026-05-13T19:30:00",
      "scheduleSpareTime": 20,
      "scheduleNote": "Write a message.",
      "latenessTime": -1,
      "doneStatus": "NOT_ENDED",
      "startedAt": "2026-05-13T10:15:30Z",
      "finishedAt": null
    },
    "preparations": [
      {
        "preparationId": "123e4567-e89b-12d3-a456-426614174011",
        "preparationName": "Wash up",
        "preparationTime": 10,
        "nextPreparationId": "123e4567-e89b-12d3-a456-426614174012"
      },
      {
        "preparationId": "123e4567-e89b-12d3-a456-426614174012",
        "preparationName": "Get dressed",
        "preparationTime": 15,
        "nextPreparationId": null
      }
    ]
  }
}
```

Frontend behavior:

- After success, update local schedule state from `data.schedule`.
- Use `data.preparations` as the running preparation steps.
- Hide or disable schedule edit actions because `startedAt != null`.
- It is safe to retry this request; the endpoint is idempotent.

## Update Schedule

Existing endpoint:

```http
PUT /schedules/{scheduleId}
```

New server-side guard:

- Allowed only when `doneStatus == "NOT_ENDED"` and `startedAt == null`.
- Backend ignores incoming `isStarted`; do not send or depend on it for locking.

Started schedule error:

```json
{
  "status": "error",
  "code": "SCHEDULE_ALREADY_STARTED",
  "message": "Started schedules cannot be edited.",
  "data": null
}
```

Finished schedule error:

```json
{
  "status": "error",
  "code": "SCHEDULE_ALREADY_FINISHED",
  "message": "Finished schedules cannot be edited.",
  "data": null
}
```

## Update Schedule-Specific Preparation

Existing endpoints:

```http
POST /schedules/{scheduleId}/preparations
PUT /schedules/{scheduleId}/preparations
```

New server-side guard:

- Allowed only when `doneStatus == "NOT_ENDED"` and `startedAt == null`.
- Returns `409 SCHEDULE_ALREADY_STARTED` after preparation has started.
- Returns `409 SCHEDULE_ALREADY_FINISHED` after the schedule is finished.

Reason:

- Editing steps or durations after start would invalidate the active preparation flow.

## Delete Schedule

Existing endpoint:

```http
DELETE /schedules/{scheduleId}
```

Rule:

- Allowed when `doneStatus == "NOT_ENDED"`.
- Started but unfinished schedules can be deleted.
- Finished schedules cannot be deleted.

Finished schedule error:

```json
{
  "status": "error",
  "code": "SCHEDULE_ALREADY_FINISHED",
  "message": "Finished schedules cannot be edited.",
  "data": null
}
```

## Default Preparation Updates

Existing user-default preparation update remains allowed.

```http
PUT /preparations
```

Frontend behavior:

- Users may edit default preparation in settings even if they have already started a schedule.
- This updates only the default template.
- It does not change any started schedule's frozen preparation snapshot.
- Future or unstarted schedules that still use default preparation may resolve the updated default template.

## Finish Preparation

Existing endpoint:

```http
PUT /schedules/{scheduleId}/finish
```

New server-side guard:

- Allowed only after explicit start, when `startedAt != null`.
- Unstarted missed schedules remain `NOT_ENDED` and do not count toward punctuality score.
- Finished schedules still return `409 SCHEDULE_ALREADY_FINISHED`.

Unstarted schedule error:

```json
{
  "status": "error",
  "code": "SCHEDULE_NOT_STARTED",
  "message": "Schedules must be started before they can be finished.",
  "data": null
}
```

Punctuality scoring rule:

```text
includedInPunctualityScore =
  startedAt != null
  && finishedAt != null
  && doneStatus in (NORMAL, LATE)
```

So a schedule with:

```text
doneStatus == NOT_ENDED
startedAt == null
scheduleTime < now
```

is a missed/unstarted schedule. It can be deleted, but it is not auto-finished and does not affect punctuality score.

`ABNORMAL` is also excluded from punctuality score. It is reserved for abnormal completion states and should not improve or worsen the score.

## Alarm Window Response

`GET /schedules/alarm-window` also includes `startedAt` and `finishedAt` for each schedule.

```json
{
  "scheduleId": "3fa85f64-5717-4562-b3fc-2c963f66afe5",
  "scheduleName": "Morning meeting",
  "scheduleTime": "2026-05-13T09:30:00",
  "moveTime": 20,
  "scheduleSpareTime": 10,
  "doneStatus": "NOT_ENDED",
  "startedAt": "2026-05-13T08:15:30Z",
  "finishedAt": null,
  "preparationStartTime": "2026-05-13T08:40:00",
  "defaultAlarmTime": "2026-05-13T08:30:00",
  "preparations": []
}
```

## Migration Notes For Flutter

Recommended client changes:

- Read `startedAt` from schedule responses.
- Read `finishedAt` when displaying explicit completion state.
- Treat `startedAt != null` as "preparation has started".
- Stop treating `isStarted` as authoritative.
- Call `POST /schedules/{id}/start` only when the user explicitly taps "Start preparation".
- On start success, replace local running preparation state with `data.preparations`.
- Hide or disable schedule/preparation edit actions when `startedAt != null`.
- Hide or disable edit/delete actions when `doneStatus != "NOT_ENDED"`, except delete is still allowed for started schedules if `doneStatus == "NOT_ENDED"`.
- Do not call finish for schedules that never successfully started.
