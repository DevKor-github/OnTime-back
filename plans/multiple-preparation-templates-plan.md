# Multiple Preparation Templates Plan

## Goal
Add support for multiple user-defined preparation templates while preserving the existing fixed default preparation flow and current schedule state policy.

Users should be able to:
- Keep one fixed default preparation set with no custom name.
- Create up to 20 active named preparation templates.
- Choose fixed default, a named template, or custom preparations when creating or updating a schedule.
- Soft-delete named templates without breaking schedules that already reference them.
- Keep started schedules frozen so later default/template edits do not mutate in-progress preparation data.

## Context
The current backend has one fixed default preparation chain per user in `preparation_user`. Schedule-specific preparation chains live in `preparation_schedule`.

Relevant code:
- `PreparationUserService` and `PreparationUserController` manage the fixed default preparations through `/preparations`.
- `PreparationScheduleService` and `PreparationScheduleController` manage schedule-specific preparations through `/schedules/{scheduleId}/preparations`.
- `ScheduleService` currently reads default preparations unless the schedule is started or `isChange = true`.
- `Schedule.startSchedule` freezes preparation data into `preparation_schedule` rows when a schedule starts.
- `ScheduleService.assertScheduleEditable` rejects edits after `startedAt != null` or after finish.
- `ScheduleService.deleteSchedule` allows deleting not-finished schedules, including started but unfinished schedules.

Current state policy to preserve:
- Finished means `finishedAt != null` or `doneStatus != NOT_ENDED`.
- Started means `startedAt != null`.
- Schedule detail updates are allowed only when not finished and not started.
- Schedule preparation updates are allowed only when not finished and not started.
- Schedule deletion is allowed when not finished, even if started.
- Finish is allowed only when started and not finished.

## Decisions
- Keep the fixed default preparation separate from named templates. It remains stored in `preparation_user`, has no custom name, and is managed by the existing `/preparations` endpoints.
- Add named preparation templates in new tables, separate from `preparation_user`.
- `GET /preparation-templates` returns only active named templates, not the fixed default.
- The fixed default is selected for schedule create when neither `preparationTemplateId` nor `customPreparations` is sent.
- Named templates use `deletedAt` soft delete. Deleted templates are hidden from the list, immutable, unavailable for new schedule selection, but still readable by direct ID for the owner.
- Existing schedules that reference a soft-deleted template keep that reference during ordinary schedule detail edits.
- Schedule metadata should include `preparationTemplateDeleted`, read live from the template row, so clients can show deleted linked templates as disabled/unavailable.
- Do not hard-delete soft-deleted templates while any schedule still references them. A future retention cleanup may hard-delete only unreferenced deleted templates.
- Account deletion should cascade through named templates, template steps, schedules, and schedule preparation snapshots.
- Privacy/account-deletion docs and tests should mention named preparation templates as preparation data.
- Soft-deleting a template does not reschedule notifications because existing linked schedules continue to resolve the same template data.
- Deleted template names are reusable by new active templates.
- Active template names are unique per user after trimming and case-insensitive normalization.
- Template names and preparation step names should be trimmed before validation/storage.
- Empty or whitespace-only template names are invalid.
- Duplicate preparation step names are allowed, including identical name/duration pairs.
- Active named templates are capped at 20 per user, excluding the fixed default.
- Each template/custom preparation list must contain 1 to 50 steps.
- Each step requires `1 <= preparationTime <= 1440`.
- Total preparation time per template/custom list must be at most 1440 minutes.
- Malformed ordered payloads, duplicate request step IDs, duplicate/gapped `orderIndex`, and malformed linked-list payloads should return `INVALID_INPUT`.
- Linked-list compatibility payloads must have exactly one head, no cycles, no disconnected nodes, no duplicate IDs, and `nextPreparationId` values only within the payload.
- New template APIs use ordered steps with zero-based contiguous `orderIndex`.
- Ordered payload arrays may arrive in any array order. Backend validates indexes, sorts by `orderIndex`, and returns sorted responses.
- Existing step IDs may be reused within the same default list, same template, or same custom schedule update, including when the step order changes.
- Client-provided preparation step UUIDs must be globally unique across all preparation step tables unless they already belong to the same resource being updated.
- Cross-table step ID collision checks can be service-level validation; table primary keys still enforce within-table uniqueness.
- Template IDs only need to be unique within the `preparation_template` table. Soft-deleted template IDs remain reserved and cannot be reused.
- Named template steps do not need legacy `nextPreparationId`.
- Existing fixed default and schedule-specific tables get `order_index`, but keep `next_preparation_id` temporarily.
- New writes to fixed default and schedule-specific preparations should maintain both `order_index` and temporary `next_preparation_id`.
- Existing compatibility endpoints keep accepting/returning linked-list-shaped `PreparationDto`.
- Compatibility responses synthesize `nextPreparationId` from order where needed.
- Bad legacy linked-list data should fail migration loudly rather than guessing order.
- Add explicit schedule preparation state:
  - `preparationMode`: `DEFAULT`, `TEMPLATE`, or `CUSTOM`.
  - `preparationTemplateId`: nullable, non-null only for `TEMPLATE`.
  - `preparationTemplateDeleted`: true only when a `TEMPLATE` schedule references a soft-deleted template.
  - `preparationFrozen`: computed as `startedAt != null`.
- Deprecate `isChange`; keep temporarily for migration/backward compatibility, but new behavior should use `preparationMode`.
- Schedule create infers mode:
  - no `preparationTemplateId` and no `customPreparations`: `DEFAULT`.
  - only `preparationTemplateId`: `TEMPLATE`.
  - only `customPreparations`: `CUSTOM`.
  - both present: reject.
- Schedule update changes preparation source only when `preparationMode` is explicitly sent:
  - `DEFAULT`: no template ID, no custom list.
  - `TEMPLATE`: requires active template ID, no custom list.
  - `CUSTOM`: requires full custom list, no template ID.
  - omitted: leave current preparation source unchanged.
- Schedule responses include metadata but not full steps in normal list/detail:
  - `preparationMode`
  - `preparationTemplateId`
  - `preparationTemplateName`
  - `preparationTemplateDeleted`
  - `preparationFrozen`
- Schedule responses should expose both raw `startedAt`/`finishedAt` timestamps and the convenience `preparationFrozen` flag.
- `alarm-window` responses include the same metadata and continue to include full preparations.
- `StartScheduleResponseDto` inherits metadata through `ScheduleDto`.
- Started schedules keep their original `preparationMode`; they do not become `CUSTOM` just because snapshot rows exist.
- When a not-started schedule switches away from `CUSTOM`, delete old custom `preparation_schedule` rows.
- When a schedule customizes away from a template, clear `preparationTemplateId`.
- Template/default changes affect only schedules that are not finished and not started:
  - `doneStatus = NOT_ENDED`
  - `startedAt IS NULL`
- Past-notification behavior should be delegated to the existing notification scheduling logic rather than inventing a new policy.
- Started schedules are never mutated by default/template updates.
- Template name-only changes do not need notification timing recalculation.
- Step content/order/time changes should refresh affected notifications even if notification time is unchanged.
- Add a dedicated notification refresh/reschedule helper rather than relying only on the existing equal-time early return.
- Existing `isChange = true` schedules migrate to `CUSTOM` because old data cannot reliably distinguish user-custom rows from auto-snapshotted rows.
- Existing `isChange = false` schedules migrate to `DEFAULT`.
- Document the migration compromise for old started schedules that originally came from default but had `isChange = true`.
- Add template-specific error codes for not found, duplicate name or ID conflict, active limit exceeded, deleted template mutation/selection, and step ID conflict.
- Cross-user template IDs should behave as not found to avoid resource enumeration.
- Owned-but-deleted templates should be readable by direct detail endpoint, rejected for create/update schedule selection, rejected for update, and idempotently accepted for repeated delete.
- Selecting an owned deleted template should use a deleted-specific error; missing/cross-user templates should use not found.
- Template creation/update uses last-write-wins for the first implementation; do not add optimistic locking yet.
- Schedule create/update should validate active template status inside its transaction. If a template is deleted immediately after a schedule links to it, the schedule keeps the link and future reads show `preparationTemplateDeleted = true`.
- Templates should have `createdAt` and `updatedAt`, and soft delete should set both `deletedAt` and `updatedAt`.
- Template list ordering should be deterministic, preferably by `createdAt` ascending with a stable tiebreaker.
- Template steps do not need individual timestamps while steps are full-replaced.

## Steps
1. Add ordering to existing preparation tables.
   - Add `order_index` to `preparation_user`.
   - Add `order_index` to `preparation_schedule`.
   - Backfill by traversing each legacy `next_preparation_id` chain from its head.
   - Fail migration if a chain has cycles, multiple heads, disconnected nodes, duplicate order, or invalid references.
   - Keep `next_preparation_id` columns temporarily.

2. Update fixed default preparation reads/writes.
   - Update repositories to read `preparation_user` by `order_index`.
   - Keep `/preparations` request/response as linked-list-shaped `PreparationDto`.
   - Convert incoming linked-list payloads to contiguous order.
   - Maintain temporary `next_preparation_id` links on writes.
   - Validate step count, positive step durations, total duration, duplicate IDs, and ownership/collision rules.

3. Update schedule-specific preparation reads/writes.
   - Update repositories to read `preparation_schedule` by `order_index`.
   - Keep old `/schedules/{scheduleId}/preparations` request/response shape.
   - Treat old schedule-preparation POST/PUT as `CUSTOM` mode compatibility endpoints.
   - Validate schedule editability before writing custom rows.
   - Maintain temporary `next_preparation_id` links on writes.

4. Add named template schema.
   - Create `preparation_template` with client-provided UUID primary key, user FK, `template_name`, `created_at`, `updated_at`, and `deleted_at`.
   - Create `preparation_template_step` with client-provided UUID primary key, template FK, name, time, and `order_index`.
   - Add indexes for user/template lookup and ordered step reads.
   - Enforce active template name uniqueness per user after trim/case normalization in service logic and DB support where practical.
   - Enforce active template count limit of 20 per user.

5. Add template DTOs, repository, service, and controller.
   - `GET /preparation-templates`: active templates with full ordered step lists, deterministic ordering, `createdAt`, and `updatedAt`; omit `deletedAt`.
   - `GET /preparation-templates/{templateId}`: direct owner lookup, including soft-deleted templates, full steps, `createdAt`, `updatedAt`, and `deletedAt`.
   - `POST /preparation-templates`: create active named template with full ordered steps.
   - `PUT /preparation-templates/{templateId}`: full replace of name and steps; reject deleted templates.
   - `DELETE /preparation-templates/{templateId}`: soft delete; always allowed for owned named templates; repeated delete is idempotent; no notification changes.
   - Trim template/step names and validate normalized active-name uniqueness.
   - Reject duplicate request step IDs, cross-resource step ID collisions, non-contiguous order, and invalid durations.

6. Add explicit schedule preparation mode.
   - Add `preparation_mode` to `schedule`, required after migration.
   - Add nullable `preparation_template_id` FK to named templates.
   - Backfill `CUSTOM` for existing `is_change = true`.
   - Backfill `DEFAULT` for existing `is_change = false` or null.
   - Keep `is_change` temporarily but stop treating it as source of truth for new behavior.

7. Update schedule create.
   - Add `preparationTemplateId` and ordered `customPreparations` to create DTO.
   - Reject payloads that include both.
   - If neither is present, create `DEFAULT` schedule.
   - If template ID is present, verify owner and active status in the transaction, then create `TEMPLATE` schedule.
   - If custom preparations are present, create `CUSTOM` schedule and write `preparation_schedule` rows immediately.
   - Recalculate and create notification from the selected source.

8. Update schedule modify.
   - Add optional `preparationMode`, `preparationTemplateId`, and ordered `customPreparations` to modify DTO.
   - Preserve current source when `preparationMode` is omitted.
   - For `DEFAULT`, clear template ID and delete pre-start custom rows.
   - For `TEMPLATE`, require active owned template ID, clear custom rows, and set template ID.
   - For `CUSTOM`, require full custom list, clear template ID, and replace custom rows.
   - Reject mixed mode payloads.
   - Preserve a soft-deleted template reference when only ordinary schedule details change and `preparationMode` is omitted.
   - Allow schedules that reference deleted templates to switch normally to `DEFAULT`, an active `TEMPLATE`, or `CUSTOM`.
   - Preserve existing started/finished edit restrictions.
   - Refresh or reschedule notifications after source or step changes.

9. Update preparation resolution.
   - For not-started schedules:
     - `DEFAULT`: read `preparation_user`.
     - `TEMPLATE`: read named template steps, including soft-deleted referenced templates.
     - `CUSTOM`: read `preparation_schedule`.
   - For started schedules:
     - Always read frozen `preparation_schedule` snapshot rows.
   - Continue returning old linked-list-shaped `PreparationDto` from existing schedule preparation reads.

10. Update start snapshot behavior.
    - Preserve original `preparationMode`.
    - For `DEFAULT`, delete any stale non-custom rows and snapshot fixed default into `preparation_schedule`.
    - For `TEMPLATE`, delete any stale non-custom rows and snapshot the referenced template into `preparation_schedule`.
    - For `CUSTOM`, leave existing custom rows unchanged.
    - Set `startedAt` as today; `preparationFrozen` is computed from it.

11. Update notification refresh/reschedule.
    - Add a helper that recalculates notification time and can force refresh payloads when step content/order changes but time is unchanged.
    - Use it after template step changes for affected `TEMPLATE` schedules where `doneStatus = NOT_ENDED` and `startedAt IS NULL`.
    - Use it after fixed default step changes for affected `DEFAULT` schedules where `doneStatus = NOT_ENDED` and `startedAt IS NULL`.
    - Use it after schedule custom preparation changes.
    - Inspect `NotificationService` and native alarm status flow before choosing the exact payload refresh mechanism.

12. Update response DTOs and API docs.
    - Add `preparationMode`, `preparationTemplateId`, `preparationTemplateName`, `preparationTemplateDeleted`, and `preparationFrozen` to `ScheduleDto`.
    - Add the same metadata to `AlarmWindowScheduleDto`.
    - Ensure `preparationTemplateName` and `preparationTemplateDeleted` are read live from the template row, including soft-deleted references.
    - Update Swagger examples for schedule create/update, schedule responses, alarm-window, and template endpoints.

13. Update account deletion, privacy, and repair flows.
    - Ensure account deletion cascades remove preparation templates and template steps.
    - Update privacy policy/account deletion evidence to include named preparation templates.
    - Adapt `repairStartedSchedulePreparationSnapshots()` to the new modes.
    - For old migrated `CUSTOM` schedules without rows, report or handle carefully rather than guessing hidden source intent.

14. Add tests.
    - Migration/order backfill behavior where practical.
    - Existing `/preparations` compatibility.
    - Existing `/schedules/{id}/preparations` compatibility and `CUSTOM` mapping.
    - Template CRUD, validation, active-name uniqueness after trim/case normalization, active cap, deterministic ordering, timestamps, soft delete, repeated delete idempotency, deleted detail lookup, and deleted update rejection.
    - Cross-user template lookups return not found.
    - Step ID collision rules across default, schedule custom, and template step tables.
    - Schedule create modes and mixed payload rejection.
    - Schedule update mode switching and custom row cleanup.
    - Schedules referencing deleted templates preserve references on ordinary edits and can switch source normally.
    - Started schedule freeze behavior for `DEFAULT`, `TEMPLATE`, and `CUSTOM`.
    - Default/template updates affect only not-finished and not-started schedules.
    - Step content changes refresh notifications even when notification time is unchanged.
    - Existing delete/edit/finish/start schedule state policy remains intact.
    - Account deletion cascades named templates and template steps.

15. Plan later cleanup.
    - Remove or fully ignore `is_change` after clients no longer depend on it.
    - Remove `next_preparation_id` from fixed default and schedule-specific tables after compatibility endpoints migrate.
    - Consider replacing old linked-list DTOs with ordered DTOs in a versioned API.

## Validation
- Run the backend test suite:
  - `./gradlew test` from `ontime-back/`.
- Run migration tests or a local Flyway migration against representative legacy data.
- Manually verify these API flows:
  - Onboarding/default preparation still works through `/preparations`.
  - Create/list/update/delete named templates.
  - Create schedules in `DEFAULT`, `TEMPLATE`, and `CUSTOM` mode.
  - Update a not-started schedule between modes.
  - Start a schedule and confirm preparations freeze.
  - Soft-delete a template and confirm existing linked schedules still resolve it, while new selection rejects it.
  - Confirm schedules linked to deleted templates show `preparationTemplateDeleted = true`.
  - Update default/template steps and confirm only not-started, not-finished schedules are refreshed.

## Open Questions
- Exact notification payload refresh mechanism after equal-time step content changes. Inspect `NotificationService` and native alarm status flow before implementation.
- Whether DB-level partial uniqueness for active template names is available in the deployed MySQL version or should remain service-enforced with locking.
- Whether to introduce a versioned ordered preparation read endpoint for new clients, or keep only template APIs ordered for the first release.
