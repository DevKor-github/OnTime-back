# Clean Transition Plan For Preparation Templates And Schedule Preparation Modes

## Context
The backend is adding multiple named preparation templates while keeping the existing fixed default preparation flow. This transition needs to be intentionally staged because existing clients still use linked-list preparation payloads (`nextPreparationId`) and the old `isChange` concept, while new clients should use explicit schedule preparation modes and ordered preparation steps.

This issue tracks the clean transition work after the initial implementation lands.

## Product Model To Preserve
- Every user has one fixed default preparation set.
- The fixed default has no custom name and is managed through the existing `/users/preparations` compatibility endpoints.
- Users can create up to 20 active named preparation templates.
- Schedules can use exactly one preparation source: `DEFAULT`, `TEMPLATE`, or `CUSTOM`.
- Named templates are soft-deleted with `deletedAt`.
- Soft-deleted templates are hidden from future selection but remain resolvable for schedules that already reference them.
- Started schedules are frozen. Their response keeps the original preparation source metadata, but steps are read from schedule snapshot rows.

## New Frontend Contract To Roll Out
Template APIs:
- `GET /preparation-templates`: active named templates only, excluding fixed default, with full ordered steps.
- `GET /preparation-templates/{templateId}`: owner-only direct lookup, including deleted templates and `deletedAt`.
- `POST /preparation-templates`: create a named template with client-provided template and step UUIDs.
- `PUT /preparation-templates/{templateId}`: full replace of name and steps; deleted templates are immutable.
- `DELETE /preparation-templates/{templateId}`: soft delete; repeated delete succeeds.

Ordered step shape:
```json
{
  "preparationId": "uuid",
  "preparationName": "Pack laptop",
  "preparationTime": 5,
  "orderIndex": 0
}
```

Schedule create modes:
- Neither `preparationTemplateId` nor `customPreparations`: create `DEFAULT`.
- Only `preparationTemplateId`: create `TEMPLATE`.
- Only `customPreparations`: create `CUSTOM`.
- Both fields: reject.

Schedule update modes:
- Omit `preparationMode`: keep current source unchanged.
- `DEFAULT`: no template ID, no custom list.
- `TEMPLATE`: requires active owned template ID, no custom list.
- `CUSTOM`: requires full custom list, no template ID.

Schedule response metadata:
```json
{
  "preparationMode": "TEMPLATE",
  "preparationTemplateId": "uuid-or-null",
  "preparationTemplateName": "Work",
  "preparationTemplateDeleted": false,
  "preparationFrozen": false,
  "startedAt": null,
  "finishedAt": null
}
```

## Compatibility Endpoints To Keep During Transition
Keep these linked-list shaped:
- `GET /users/preparations`
- `PUT /users/preparations`
- `GET /schedules/{scheduleId}/preparations`
- `POST /schedules/{scheduleId}/preparations`
- `PUT /schedules/{scheduleId}/preparations`

Compatibility behavior:
- Existing request/response shape uses `nextPreparationId`.
- Backend stores/maintains `orderIndex` internally.
- Backend synthesizes `nextPreparationId` from order on compatibility reads.
- Old schedule-preparation POST/PUT maps the schedule to `CUSTOM`, clears any template link, and replaces schedule-specific rows.

## Migration And Rollout Checklist
Phase 1: Backend compatibility release
- [ ] Add `order_index` to `preparation_user` and `preparation_schedule`.
- [ ] Backfill existing rows.
- [ ] Keep `next_preparation_id` during transition.
- [ ] Add `preparation_mode` to `schedule`.
- [ ] Add nullable `preparation_template_id` to `schedule`.
- [ ] Migrate old `is_change = true` schedules to `CUSTOM`.
- [ ] Migrate old `is_change = false` schedules to `DEFAULT`.
- [ ] Document the compromise that historical started default snapshots with `is_change = true` become `CUSTOM` because source intent cannot be reliably recovered.
- [ ] Add template tables and endpoints.
- [ ] Add schedule response metadata.
- [ ] Keep old endpoints working.

Phase 2: Frontend adoption
- [ ] Update template picker to show local fixed default option plus named templates from `/preparation-templates`.
- [ ] Treat missing `preparationTemplateId` plus no custom list as fixed default on create.
- [ ] Use `preparationMode` for schedule update source changes.
- [ ] Use ordered `customPreparations` for new custom schedule create/update.
- [ ] Continue using old linked-list endpoints only where necessary.
- [ ] Show deleted linked templates as disabled/unavailable when `preparationTemplateDeleted = true`.
- [ ] Prevent selecting deleted templates from the picker.
- [ ] Generate new step UUIDs when copying a template into custom schedule steps.
- [ ] Respect `preparationFrozen = true` by disabling preparation edits on started schedules.

Phase 3: Monitoring and validation
- [ ] Verify old app versions can still onboard and update `/users/preparations`.
- [ ] Verify old app versions can still create schedule-specific preparations via `/schedules/{id}/preparations`.
- [ ] Verify new app can create `DEFAULT`, `TEMPLATE`, and `CUSTOM` schedules.
- [ ] Verify template updates refresh not-started, not-finished template-mode schedules.
- [ ] Verify default preparation updates refresh not-started, not-finished default-mode schedules.
- [ ] Verify started schedules keep frozen snapshot steps.
- [ ] Verify deleted templates remain visible through schedule metadata and direct detail lookup.
- [ ] Verify account deletion removes named templates and template steps.
- [ ] Verify privacy/account deletion docs mention named templates.

Phase 4: Cleanup after client migration
- [ ] Stop documenting `isChange` for new clients.
- [ ] Remove client use of linked-list `nextPreparationId` from new app code.
- [ ] Add a versioned ordered preparation read endpoint if frontend needs ordered schedule/default reads without linked-list compatibility.
- [ ] Once old clients are no longer supported, remove or fully ignore `is_change`.
- [ ] Once old linked-list endpoints are retired, remove `next_preparation_id` from `preparation_user` and `preparation_schedule`.
- [ ] Remove compatibility synthesis code for `nextPreparationId`.
- [ ] Simplify repository queries to rely only on `order_index`.

## Edge Cases To Test Explicitly
- Creating a template with duplicate active name after trim/case normalization is rejected.
- Creating a template after deleting another template with the same name succeeds.
- Creating a template with a deleted template's same ID is rejected.
- Deleting a template twice succeeds.
- Updating a deleted template is rejected.
- Selecting an owned deleted template for schedule create/update returns a deleted-specific error.
- Selecting another user's template behaves as not found.
- Schedule detail edit with omitted `preparationMode` preserves a deleted template reference.
- Schedule linked to deleted template can switch to default, active template, or custom.
- Custom schedule update can reuse its own step IDs and reorder them.
- Custom schedule update cannot use step IDs from templates, fixed default, or another schedule.
- Template update can reuse its own step IDs and reorder them.
- Fixed default update can reuse its own step IDs.
- Malformed linked-list payloads are rejected: cycles, multiple heads, disconnected nodes, unknown next IDs, duplicate IDs.
- Malformed ordered payloads are rejected: duplicate/gapped indexes, duplicate IDs, empty list, more than 50 steps, total duration over 1440.
- Equal-time step content changes still refresh notifications without leaving duplicate scheduled tasks.

## Open Implementation Notes
- Confirm whether DB-level active-name uniqueness can be enforced cleanly in the deployed MySQL version; service-level validation is still required.
- Confirm native alarm payload refresh behavior when notification time does not change but step names/order do.
- Keep `docs/preparation-templates-frontend.md` aligned with actual endpoint behavior.
