# Preparation Templates Frontend Contract

## Concepts
Schedules now have one preparation source:

- `DEFAULT`: uses the user's fixed default preparation from `GET /users/preparations`.
- `TEMPLATE`: uses a named preparation template from `GET /preparation-templates`.
- `CUSTOM`: uses schedule-specific preparation steps.

Started schedules are frozen. Use `preparationFrozen` from schedule responses; it is `true` when `startedAt` is present. Frozen schedules still show their original `preparationMode`, but their preparation steps come from the schedule snapshot.

## Ordered Preparation Shape
New APIs use ordered steps:

```json
{
  "preparationId": "3fa85f64-5717-4562-b3fc-2c963f66afe5",
  "preparationName": "Shower",
  "preparationTime": 15,
  "orderIndex": 0
}
```

Rules:
- Client provides UUIDs for templates and preparation steps.
- `orderIndex` is zero-based and contiguous.
- Arrays may be sent in any order; backend stores and returns by `orderIndex`.
- Each list must contain 1-50 steps.
- Each `preparationTime` must be 1-1440 minutes.
- Total preparation time per list must be at most 1440 minutes.
- Step names are trimmed; duplicate step names are allowed.
- Step IDs must not be reused across other templates, other schedules, or the fixed default. Reusing the same step ID within the same resource update is allowed.

## Named Template APIs
The fixed default preparation is not included in these endpoints.

### List Active Templates
`GET /preparation-templates`

Returns active named templates with full steps. Deleted templates are excluded.

```json
{
  "status": "success",
  "data": [
    {
      "templateId": "11111111-1111-1111-1111-111111111111",
      "templateName": "Work",
      "createdAt": "2026-05-14T02:10:00Z",
      "updatedAt": "2026-05-14T02:10:00Z",
      "deletedAt": null,
      "preparations": []
    }
  ]
}
```

### Get Template Detail
`GET /preparation-templates/{templateId}`

Works for active and soft-deleted templates owned by the user. Detail includes `deletedAt`.

### Create Template
`POST /preparation-templates`

```json
{
  "templateId": "11111111-1111-1111-1111-111111111111",
  "templateName": "Work",
  "preparations": [
    {
      "preparationId": "22222222-2222-2222-2222-222222222222",
      "preparationName": "Pack laptop",
      "preparationTime": 5,
      "orderIndex": 0
    }
  ]
}
```

Active template names are unique per user after trimming and case-insensitive normalization. Deleted template names can be reused, but deleted template IDs cannot.

### Update Template
`PUT /preparation-templates/{templateId}`

Full replace of name and steps. Deleted templates cannot be updated.

### Delete Template
`DELETE /preparation-templates/{templateId}`

Soft-deletes the template. Existing schedules that already use the template keep using it. New schedule create/update cannot select it. Repeated delete succeeds.

## Schedule Create
`POST /schedules`

Preparation source is inferred:

- Omit both `preparationTemplateId` and `customPreparations`: creates `DEFAULT`.
- Send only `preparationTemplateId`: creates `TEMPLATE`.
- Send only `customPreparations`: creates `CUSTOM`.
- Send both: rejected.

Template mode:

```json
{
  "scheduleId": "33333333-3333-3333-3333-333333333333",
  "placeId": "44444444-4444-4444-4444-444444444444",
  "placeName": "Office",
  "scheduleName": "Morning meeting",
  "moveTime": 20,
  "scheduleTime": "2026-06-01T09:30:00",
  "scheduleSpareTime": 10,
  "scheduleNote": "Bring laptop",
  "preparationTemplateId": "11111111-1111-1111-1111-111111111111"
}
```

Custom mode:

```json
{
  "scheduleId": "33333333-3333-3333-3333-333333333333",
  "placeId": "44444444-4444-4444-4444-444444444444",
  "placeName": "Office",
  "scheduleName": "Morning meeting",
  "moveTime": 20,
  "scheduleTime": "2026-06-01T09:30:00",
  "customPreparations": [
    {
      "preparationId": "55555555-5555-5555-5555-555555555555",
      "preparationName": "Pack laptop",
      "preparationTime": 5,
      "orderIndex": 0
    }
  ]
}
```

## Schedule Update
`PUT /schedules/{scheduleId}`

If `preparationMode` is omitted, the current preparation source is preserved. This includes schedules linked to soft-deleted templates.

To change source:

```json
{
  "placeId": "44444444-4444-4444-4444-444444444444",
  "placeName": "Office",
  "scheduleName": "Morning meeting",
  "moveTime": 20,
  "scheduleTime": "2026-06-01T09:30:00",
  "preparationMode": "DEFAULT"
}
```

```json
{
  "placeId": "44444444-4444-4444-4444-444444444444",
  "placeName": "Office",
  "scheduleName": "Morning meeting",
  "moveTime": 20,
  "scheduleTime": "2026-06-01T09:30:00",
  "preparationMode": "TEMPLATE",
  "preparationTemplateId": "11111111-1111-1111-1111-111111111111"
}
```

```json
{
  "placeId": "44444444-4444-4444-4444-444444444444",
  "placeName": "Office",
  "scheduleName": "Morning meeting",
  "moveTime": 20,
  "scheduleTime": "2026-06-01T09:30:00",
  "preparationMode": "CUSTOM",
  "customPreparations": [
    {
      "preparationId": "55555555-5555-5555-5555-555555555555",
      "preparationName": "Pack laptop",
      "preparationTime": 5,
      "orderIndex": 0
    }
  ]
}
```

Mixed mode payloads are rejected:
- `DEFAULT` with template ID or custom list.
- `TEMPLATE` without template ID.
- `TEMPLATE` with custom list.
- `CUSTOM` without full custom list.
- `CUSTOM` with template ID.

Started schedules cannot be edited.

## Schedule Responses
Normal schedule list/detail responses include metadata, not full step lists:

```json
{
  "scheduleId": "33333333-3333-3333-3333-333333333333",
  "scheduleName": "Morning meeting",
  "startedAt": null,
  "finishedAt": null,
  "preparationMode": "TEMPLATE",
  "preparationTemplateId": "11111111-1111-1111-1111-111111111111",
  "preparationTemplateName": "Work",
  "preparationTemplateDeleted": false,
  "preparationFrozen": false
}
```

For default and custom schedules, `preparationTemplateId` and `preparationTemplateName` are `null`.

For schedules linked to a deleted template:

```json
{
  "preparationMode": "TEMPLATE",
  "preparationTemplateId": "11111111-1111-1111-1111-111111111111",
  "preparationTemplateName": "Work",
  "preparationTemplateDeleted": true
}
```

## Existing Compatibility Endpoints
These remain linked-list shaped:

- `GET /users/preparations`
- `PUT /users/preparations`
- `GET /schedules/{scheduleId}/preparations`
- `POST /schedules/{scheduleId}/preparations`
- `PUT /schedules/{scheduleId}/preparations`

`POST/PUT /schedules/{scheduleId}/preparations` now means "make this schedule CUSTOM" and clears any template link. The request still uses `nextPreparationId`.

## Alarm Window
`GET /schedules/alarm-window` continues to include full `preparations`, and now also includes the same preparation metadata fields as normal schedule responses.

## Error Codes To Handle
- `PREPARATION_TEMPLATE_NOT_FOUND`: missing or cross-user template.
- `PREPARATION_TEMPLATE_NAME_DUPLICATE`: active template name already exists.
- `PREPARATION_TEMPLATE_LIMIT_EXCEEDED`: user already has 20 active named templates.
- `PREPARATION_TEMPLATE_DELETED`: user tried to select or update an owned deleted template.
- `PREPARATION_STEP_ID_CONFLICT`: provided step ID belongs to another preparation resource.
- `INVALID_INPUT`: malformed mode combination, ordering, list size, duration, or linked-list payload.
