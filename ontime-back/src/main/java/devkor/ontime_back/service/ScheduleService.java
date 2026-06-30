package devkor.ontime_back.service;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.entity.*;
import devkor.ontime_back.repository.*;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static devkor.ontime_back.response.ErrorCode.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final UserService userService;
    private final NotificationService notificationService;
    private final AlarmService alarmService;

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final PreparationScheduleRepository preparationScheduleRepository;
    private final PreparationUserRepository preparationUserRepository;
    private final PreparationTemplateRepository preparationTemplateRepository;
    private final PreparationTemplateStepRepository preparationTemplateStepRepository;
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final PreparationStepService preparationStepService;

    // scheduleId, userId를 통한 권한 확인
    private Schedule getScheduleWithAuthorization(UUID scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findByIdWithUser(scheduleId)
                .orElseThrow(() -> new GeneralException(SCHEDULE_NOT_FOUND));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(UNAUTHORIZED_ACCESS);
        }

        return schedule;
    }

    private Schedule getLockedScheduleWithAuthorization(UUID scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findByIdWithUserAndPlaceForUpdate(scheduleId)
                .orElseThrow(() -> new GeneralException(SCHEDULE_NOT_FOUND));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(UNAUTHORIZED_ACCESS);
        }

        return schedule;
    }

    public void assertScheduleEditable(Schedule schedule) {
        if (isFinished(schedule)) {
            throw new GeneralException(SCHEDULE_ALREADY_FINISHED);
        }
        if (schedule.getStartedAt() != null) {
            throw new GeneralException(SCHEDULE_ALREADY_STARTED);
        }
    }

    private void assertScheduleNotFinished(Schedule schedule) {
        if (isFinished(schedule)) {
            throw new GeneralException(SCHEDULE_ALREADY_FINISHED);
        }
    }

    private boolean isFinished(Schedule schedule) {
        return schedule.getFinishedAt() != null
                || (schedule.getDoneStatus() != null && schedule.getDoneStatus() != DoneStatus.NOT_ENDED);
    }

    // 특정 기간의 약속 조회
    public List<ScheduleDto> showSchedulesByPeriod(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Integer userSpareTime = userRepository.findSpareTimeById(userId);

        List<Schedule> periodScheduleList;
        if (startDate == null && endDate != null) { // StartDate가 null인 경우, EndDate 이전의 일정 모두 반환
            periodScheduleList = scheduleRepository.findAllByUserIdAndScheduleTimeBefore(userId, endDate);
        } else if (endDate == null && startDate != null) { // EndDate가 null인 경우, StartDate 이후의 일정 모두 반환
            periodScheduleList = scheduleRepository.findAllByUserIdAndScheduleTimeAfter(userId, startDate);
        } else if (startDate != null && endDate != null) { // StartDate와 EndDate 모두 존재하는 경우, 해당 기간의 일정 반환
            periodScheduleList = scheduleRepository.findAllByUserIdAndScheduleTimeBetween(
                    userId, startDate, endDate);
        } else { // StartDate와 EndDate가 모두 null인 경우, 모든 일정 반환
            periodScheduleList = scheduleRepository.findAllByUserIdWithPlace(userId);
        }

        return periodScheduleList.stream()
                .map(schedule -> {
                    ScheduleDto scheduleDto = mapToDto(schedule);
                    // schedule의 spareTime이 null이면 userSpareTime을 사용
                    scheduleDto.setScheduleSpareTime(Optional.ofNullable(schedule.getScheduleSpareTime()).orElse(userSpareTime));
                    return scheduleDto;
                })
                .collect(Collectors.toList());
    }

    // schedule id에 따른 schedule 조회
    public ScheduleDto showScheduleByScheduleId(Long userId, UUID scheduleId) {
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        return mapToDto(schedule);
    }

    // schedule 삭제
    @Transactional
    public void deleteSchedule(UUID scheduleId, Long userId) {
        Schedule schedule = getLockedScheduleWithAuthorization(scheduleId, userId);
        assertScheduleNotFinished(schedule);
        List<NotificationSchedule> notifications = notificationScheduleRepository.findAllByScheduleScheduleIdOrderByIdAsc(scheduleId);
        if (notifications.isEmpty()) {
            throw new GeneralException(NOTIFICATION_NOT_FOUND);
        }
        notifications.forEach(notification -> notificationService.cancelScheduledNotification(notification.getId()));
        scheduleRepository.deleteByScheduleId(scheduleId);
    }

    // schedule 수정
    @Transactional
    public void modifySchedule(Long userId, UUID scheduleId, ScheduleModDto scheduleModDto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
        Schedule schedule = getLockedScheduleWithAuthorization(scheduleId, userId);
        assertScheduleEditable(schedule);

        Place place = placeRepository.findByPlaceName(scheduleModDto.getPlaceName())
                .orElseGet(() -> placeRepository.save(new Place(scheduleModDto.getPlaceId(), scheduleModDto.getPlaceName())));

        schedule.updateSchedule(place, scheduleModDto);
        applyModifyPreparationMode(schedule, userId, scheduleModDto);

        scheduleRepository.save(schedule);

        refreshScheduleNotification(schedule);
    }

    public void updateAndRescheduleNotification(LocalDateTime newNotificationTime, NotificationSchedule notification) {
        if(newNotificationTime.equals(notification.getNotificationTime())) return;

        notificationService.cancelScheduledNotification(notification.getId());
        notification.updateNotificationTime(newNotificationTime);
        notification.markAsUnsent();
        notificationScheduleRepository.save(notification);
        notificationService.scheduleReminder(notification);
        log.info("{}에 대한 알림정보 업데이트되고 스케줄링 계획도 리스케줄됨", notification.getSchedule().getScheduleName());
    }

    // schedule 추가
    @Transactional
    public void addSchedule(ScheduleAddDto scheduleAddDto, Long userId) {
        if (scheduleRepository.existsById(scheduleAddDto.getScheduleId())) {
            throw new GeneralException(RESOURCE_ALREADY_EXISTS);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
        Place place = placeRepository.findByPlaceName(scheduleAddDto.getPlaceName())
                .orElseGet(() -> placeRepository.save(new Place(scheduleAddDto.getPlaceId(), scheduleAddDto.getPlaceName())));

        Schedule schedule = scheduleAddDto.toEntity(user, place);
        applyCreatePreparationMode(schedule, userId, scheduleAddDto);
        scheduleRepository.save(schedule);

        if (schedule.effectivePreparationMode() == PreparationMode.CUSTOM) {
            replaceSchedulePreparations(schedule, preparationStepService.normalizeOrdered(scheduleAddDto.getCustomPreparations()));
        }

        LocalDateTime notificationTime = getNotificationTime(schedule, user);

        NotificationSchedule notification = NotificationSchedule.builder()
                .notificationTime(notificationTime)
                .isSent(false)
                .schedule(schedule)
                .build();
        notificationScheduleRepository.save(notification);

        notificationService.scheduleReminder(notification);
    }

    @Transactional
    public StartScheduleResponseDto startSchedule(Long userId, UUID scheduleId) {
        Schedule schedule = getLockedScheduleWithAuthorization(scheduleId, userId);
        assertScheduleNotFinished(schedule);

        if (schedule.getStartedAt() == null) {
            schedule.startSchedule(nowForPersistence());
            freezePreparationSnapshotIfNeeded(schedule);
            scheduleRepository.save(schedule);
        }

        return new StartScheduleResponseDto(
                mapToDto(schedule),
                getPreparations(userId, scheduleId)
        );
    }

    private void freezePreparationSnapshotIfNeeded(Schedule schedule) {
        PreparationMode mode = schedule.effectivePreparationMode();
        if (mode == PreparationMode.CUSTOM) {
            schedule.changePreparationSchedule();
            return;
        }
        preparationScheduleRepository.deleteBySchedule(schedule);
        if (mode == PreparationMode.TEMPLATE) {
            copyTemplatePreparationsToSchedule(schedule);
        } else {
            copyDefaultPreparationsToSchedule(schedule);
        }
        preparationScheduleRepository.flush();
        schedule.changePreparationSchedule();
    }

    private void copyDefaultPreparationsToSchedule(Schedule schedule) {
        List<PreparationUser> defaultPreparations = preparationUserRepository.findByUserIdWithNextPreparation(schedule.getUser().getId());
        List<PreparationDto> linkedPreparations = preparationStepService.toLinkedDtoFromUser(defaultPreparations);
        List<OrderedPreparationDto> orderedPreparations = new java.util.ArrayList<>();
        for (int i = 0; i < linkedPreparations.size(); i++) {
            PreparationDto defaultPreparation = linkedPreparations.get(i);
            orderedPreparations.add(OrderedPreparationDto.builder()
                        .preparationId(UUID.randomUUID())
                        .preparationName(defaultPreparation.getPreparationName())
                        .preparationTime(defaultPreparation.getPreparationTime())
                        .orderIndex(i)
                        .build());
        }
        saveSchedulePreparations(schedule, orderedPreparations);
    }

    private void copyTemplatePreparationsToSchedule(Schedule schedule) {
        PreparationTemplate template = schedule.getPreparationTemplate();
        if (template == null) {
            throw new GeneralException(PREPARATION_TEMPLATE_NOT_FOUND);
        }
        List<OrderedPreparationDto> orderedPreparations = preparationTemplateStepRepository.findByPreparationTemplateOrdered(template).stream()
                .map(templateStep -> OrderedPreparationDto.builder()
                        .preparationId(UUID.randomUUID())
                        .preparationName(templateStep.getPreparationName())
                        .preparationTime(templateStep.getPreparationTime())
                        .orderIndex(defaultNonNegative(templateStep.getOrderIndex()))
                        .build())
                .collect(Collectors.toList());
        saveSchedulePreparations(schedule, orderedPreparations);
    }

    @Transactional
    public void replaceScheduleCustomPreparations(Long userId, UUID scheduleId, List<PreparationDto> preparationDtoList, boolean shouldDelete) {
        Schedule schedule = getLockedScheduleWithAuthorization(scheduleId, userId);
        assertScheduleEditable(schedule);
        List<OrderedPreparationDto> orderedPreparations = preparationStepService.normalizeLinked(preparationDtoList);
        preparationStepService.assertStepIdsAvailableForSchedule(orderedPreparations, schedule);
        if (shouldDelete || preparationScheduleRepository.existsBySchedule(schedule)) {
            preparationScheduleRepository.deleteBySchedule(schedule);
            preparationScheduleRepository.flush();
        }
        schedule.useCustomPreparation();
        scheduleRepository.save(schedule);
        saveSchedulePreparations(schedule, orderedPreparations);
        refreshScheduleNotification(schedule);
    }

    private void replaceSchedulePreparations(Schedule schedule, List<OrderedPreparationDto> orderedPreparations) {
        preparationStepService.assertStepIdsAvailableForSchedule(orderedPreparations, schedule);
        preparationScheduleRepository.deleteBySchedule(schedule);
        preparationScheduleRepository.flush();
        saveSchedulePreparations(schedule, orderedPreparations);
    }

    private void saveSchedulePreparations(Schedule schedule, List<OrderedPreparationDto> orderedPreparations) {
        Map<UUID, PreparationSchedule> preparationMap = new HashMap<>();
        List<PreparationSchedule> preparationSchedules = orderedPreparations.stream()
                .map(dto -> {
                    PreparationSchedule preparation = new PreparationSchedule(
                            dto.getPreparationId(),
                            schedule,
                            dto.getPreparationName(),
                            dto.getPreparationTime(),
                            dto.getOrderIndex(),
                            null
                    );
                    preparationMap.put(dto.getPreparationId(), preparation);
                    return preparation;
                })
                .collect(Collectors.toList());

        preparationScheduleRepository.saveAll(preparationSchedules);
        preparationScheduleRepository.flush();

        for (int i = 0; i < orderedPreparations.size() - 1; i++) {
            PreparationSchedule current = preparationMap.get(orderedPreparations.get(i).getPreparationId());
            PreparationSchedule nextPreparation = preparationMap.get(orderedPreparations.get(i + 1).getPreparationId());
            current.updateNextPreparation(nextPreparation);
        }

        preparationScheduleRepository.saveAll(preparationSchedules);
    }

    @Transactional
    public int repairStartedSchedulePreparationSnapshots() {
        List<Schedule> schedules = scheduleRepository.findStartedSchedulesWithoutPreparationSnapshot();
        schedules.forEach(schedule -> {
            freezePreparationSnapshotIfNeeded(schedule);
            scheduleRepository.save(schedule);
        });
        return schedules.size();
    }

    public LocalDateTime getNotificationTime(Schedule schedule, User user) {
        Integer preparationTime = calculatePreparationTime(schedule, user);
        Integer moveTime = defaultNonNegative(schedule.getMoveTime());
        Integer spareTime = getEffectiveSpareTime(schedule);
        Integer defaultAlarmOffsetMinutes = alarmService.getDefaultAlarmOffsetMinutes(user.getId());
        return schedule.getScheduleTime().minusMinutes(preparationTime + moveTime + spareTime + defaultAlarmOffsetMinutes);
    }

    private Integer calculatePreparationTime(Schedule schedule, User user) {
        List<PreparationDto> preparationDtos = getPreparations(user.getId(), schedule.getScheduleId());
        return preparationDtos.stream()
                .map(PreparationDto::getPreparationTime)
                .map(this::defaultNonNegative)
                .reduce(0, Integer::sum);
    }

    // 지각 히스토리 반환
    public List<LatenessHistoryResponse> getLatenessHistory(Long userId) {
        return scheduleRepository.findLatenessHistoryByUserId(userId).stream()
                .map(schedule -> new LatenessHistoryResponse(
                        schedule.getScheduleId(),
                        schedule.getScheduleName(),
                        schedule.getScheduleTime(),
                        schedule.getLatenessTime()
                ))
                .toList();
    }

    // 지각 시간 업데이트
    @Transactional
    public void updateLatenessTime(Schedule schedule, Integer latenessTime) {
        schedule.finish(latenessTime, nowForPersistence());
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void finishSchedule(Long userId, UUID scheduleId, FinishPreparationDto finishPreparationDto) {
        if (finishPreparationDto == null || finishPreparationDto.getLatenessTime() == null) {
            throw new GeneralException(INVALID_INPUT);
        }
        if (finishPreparationDto.getScheduleId() != null && !scheduleId.equals(finishPreparationDto.getScheduleId())) {
            throw new GeneralException(SCHEDULE_ID_MISMATCH);
        }

        Schedule schedule = getLockedScheduleWithAuthorization(scheduleId, userId);
        boolean alreadyFinishedByDoneStatus = schedule.getDoneStatus() != null && schedule.getDoneStatus() != DoneStatus.NOT_ENDED;
        boolean alreadyFinishedByLatenessTime = schedule.getLatenessTime() != null && schedule.getLatenessTime() != -1;
        if (schedule.getFinishedAt() != null || alreadyFinishedByDoneStatus || alreadyFinishedByLatenessTime) {
            throw new GeneralException(SCHEDULE_ALREADY_FINISHED);
        }
        if (schedule.getStartedAt() == null) {
            throw new GeneralException(SCHEDULE_NOT_STARTED);
        }

        schedule.finish(finishPreparationDto.getLatenessTime(), nowForPersistence());
        scheduleRepository.save(schedule);
        userService.updatePunctualityScore(userId, schedule.getDoneStatus());
    }

    // schedule에 따른 preparation 조회
    public List<PreparationDto> getPreparations(Long userId, UUID scheduleId) {
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        return resolvePreparationDtos(schedule);
    }

    public List<AlarmWindowScheduleDto> getAlarmWindowSchedules(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new GeneralException(INVALID_INPUT);
        }
        if (Duration.between(startDate, endDate).compareTo(Duration.ofDays(14)) > 0) {
            throw new GeneralException(ALARM_WINDOW_RANGE_TOO_LONG);
        }

        List<Schedule> schedules = scheduleRepository.findAlarmWindowSchedules(userId, startDate, endDate, DoneStatus.NOT_ENDED);
        Integer defaultAlarmOffsetMinutes = alarmService.getDefaultAlarmOffsetMinutes(userId);
        Integer userSpareTime = userRepository.findSpareTimeById(userId);
        AlarmWindowPreparationLookup preparationLookup = preloadAlarmWindowPreparations(userId, schedules);

        return schedules.stream()
                .map(schedule -> mapToAlarmWindowDto(schedule, defaultAlarmOffsetMinutes, userSpareTime, preparationLookup))
                .collect(Collectors.toList());
    }

    private ScheduleDto mapToDto(Schedule schedule) {
        return new ScheduleDto(
                schedule.getScheduleId(),
                (schedule.getPlace() != null) ? new PlaceDto(schedule.getPlace().getPlaceId(), schedule.getPlace().getPlaceName()) : null,
                schedule.getScheduleName(),
                schedule.getMoveTime(),
                schedule.getScheduleTime(),
                (schedule.getScheduleSpareTime() == null) ? schedule.getUser().getSpareTime() : schedule.getScheduleSpareTime(),
                schedule.getScheduleNote(),
                schedule.getLatenessTime(),
                schedule.getDoneStatus(),
                schedule.getStartedAt(),
                schedule.getFinishedAt(),
                schedule.effectivePreparationMode(),
                schedule.getPreparationTemplate() != null ? schedule.getPreparationTemplate().getPreparationTemplateId() : null,
                schedule.getPreparationTemplate() != null ? schedule.getPreparationTemplate().getTemplateName() : null,
                schedule.getPreparationTemplate() != null ? schedule.getPreparationTemplate().isDeleted() : false,
                schedule.getStartedAt() != null
        );
    }

    private AlarmWindowScheduleDto mapToAlarmWindowDto(Schedule schedule,
                                                       Integer defaultAlarmOffsetMinutes,
                                                       Integer userSpareTime,
                                                       AlarmWindowPreparationLookup preparationLookup) {
        List<PreparationDto> preparations = preparationLookup != null
                ? preparationLookup.resolve(schedule)
                : resolvePreparationDtos(schedule);

        int totalPreparationTime = preparations.stream()
                .map(PreparationDto::getPreparationTime)
                .map(this::defaultNonNegative)
                .reduce(0, Integer::sum);
        int moveTime = defaultNonNegative(schedule.getMoveTime());
        int scheduleSpareTime = getEffectiveSpareTime(schedule, userSpareTime);
        PreparationMode preparationMode = schedule.effectivePreparationMode();
        PreparationTemplate preparationTemplate = preparationMode == PreparationMode.TEMPLATE
                ? schedule.getPreparationTemplate()
                : null;

        LocalDateTime preparationStartTime = schedule.getScheduleTime()
                .minusMinutes((long) totalPreparationTime + moveTime + scheduleSpareTime);
        LocalDateTime defaultAlarmTime = preparationStartTime.minusMinutes(defaultNonNegative(defaultAlarmOffsetMinutes));

        return AlarmWindowScheduleDto.builder()
                .scheduleId(schedule.getScheduleId())
                .scheduleName(schedule.getScheduleName())
                .place((schedule.getPlace() != null) ? new PlaceDto(schedule.getPlace().getPlaceId(), schedule.getPlace().getPlaceName()) : null)
                .scheduleTime(schedule.getScheduleTime())
                .moveTime(moveTime)
                .scheduleSpareTime(scheduleSpareTime)
                .doneStatus(schedule.getDoneStatus())
                .startedAt(schedule.getStartedAt())
                .finishedAt(schedule.getFinishedAt())
                .preparationMode(preparationMode)
                .preparationTemplateId(preparationTemplate != null ? preparationTemplate.getPreparationTemplateId() : null)
                .preparationTemplateName(preparationTemplate != null ? preparationTemplate.getTemplateName() : null)
                .preparationTemplateDeleted(preparationTemplate != null && preparationTemplate.isDeleted())
                .preparationFrozen(schedule.getStartedAt() != null)
                .preparationStartTime(preparationStartTime)
                .defaultAlarmTime(defaultAlarmTime)
                .preparations(preparations)
                .alarmSettings(null)
                .build();
    }

    private AlarmWindowPreparationLookup preloadAlarmWindowPreparations(Long userId, List<Schedule> schedules) {
        if (schedules.isEmpty()) {
            return new AlarmWindowPreparationLookup(List.of(), Map.of(), Map.of());
        }

        boolean hasDefaultPreparationSchedule = schedules.stream()
                .anyMatch(schedule -> schedule.getStartedAt() == null
                        && schedule.effectivePreparationMode() == PreparationMode.DEFAULT);
        List<PreparationDto> defaultPreparations = List.of();
        if (hasDefaultPreparationSchedule) {
            defaultPreparations = preparationStepService.toLinkedDtoFromUser(
                    preparationUserRepository.findByUserIdWithNextPreparation(userId)
            );
        }

        List<Schedule> scheduleSpecificPreparationSchedules = schedules.stream()
                .filter(schedule -> schedule.getStartedAt() != null || schedule.effectivePreparationMode() == PreparationMode.CUSTOM)
                .toList();
        Map<UUID, List<PreparationDto>> preparationsByScheduleId = scheduleSpecificPreparationSchedules.isEmpty()
                ? Map.of()
                : preparationScheduleRepository.findBySchedulesWithNextPreparation(scheduleSpecificPreparationSchedules)
                .stream()
                .collect(Collectors.groupingBy(
                        preparationSchedule -> preparationSchedule.getSchedule().getScheduleId(),
                        Collectors.collectingAndThen(Collectors.toList(), preparationStepService::toLinkedDtoFromSchedule)
                ));

        List<PreparationTemplate> templates = schedules.stream()
                .filter(schedule -> schedule.effectivePreparationMode() == PreparationMode.TEMPLATE)
                .map(Schedule::getPreparationTemplate)
                .filter(template -> template != null)
                .collect(Collectors.toMap(
                        PreparationTemplate::getPreparationTemplateId,
                        template -> template,
                        (first, ignored) -> first
                ))
                .values()
                .stream()
                .toList();
        Map<UUID, List<PreparationDto>> preparationsByTemplateId = templates.isEmpty()
                ? Map.of()
                : preparationTemplateStepRepository.findByPreparationTemplatesOrdered(templates)
                .stream()
                .collect(Collectors.groupingBy(
                        templateStep -> templateStep.getPreparationTemplate().getPreparationTemplateId(),
                        Collectors.collectingAndThen(Collectors.toList(), preparationStepService::toLinkedDtoFromTemplate)
                ));

        return new AlarmWindowPreparationLookup(defaultPreparations, preparationsByScheduleId, preparationsByTemplateId);
    }

    private PreparationDto mapPreparationScheduleToDto(PreparationSchedule preparationSchedule) {
        return new PreparationDto(
                preparationSchedule.getPreparationScheduleId(),
                preparationSchedule.getPreparationName(),
                defaultNonNegative(preparationSchedule.getPreparationTime()),
                preparationSchedule.getNextPreparation() != null
                        ? preparationSchedule.getNextPreparation().getPreparationScheduleId()
                        : null
        );
    }

    private List<PreparationDto> resolvePreparationDtos(Schedule schedule) {
        if (schedule.getStartedAt() != null || schedule.effectivePreparationMode() == PreparationMode.CUSTOM) {
            return preparationStepService.toLinkedDtoFromSchedule(
                    preparationScheduleRepository.findByScheduleWithNextPreparation(schedule)
            );
        }
        if (schedule.effectivePreparationMode() == PreparationMode.TEMPLATE) {
            if (schedule.getPreparationTemplate() == null) {
                throw new GeneralException(PREPARATION_TEMPLATE_NOT_FOUND);
            }
            return preparationStepService.toLinkedDtoFromTemplate(
                    preparationTemplateStepRepository.findByPreparationTemplateOrdered(schedule.getPreparationTemplate())
            );
        }
        return preparationStepService.toLinkedDtoFromUser(
                preparationUserRepository.findByUserIdWithNextPreparation(schedule.getUser().getId())
        );
    }

    private void applyCreatePreparationMode(Schedule schedule, Long userId, ScheduleAddDto scheduleAddDto) {
        boolean hasTemplate = scheduleAddDto.getPreparationTemplateId() != null;
        boolean hasCustom = scheduleAddDto.getCustomPreparations() != null;
        if (hasCustom && scheduleAddDto.getCustomPreparations().isEmpty()) {
            throw new GeneralException(INVALID_INPUT);
        }
        if (hasTemplate && hasCustom) {
            throw new GeneralException(INVALID_INPUT);
        }
        if (hasTemplate) {
            schedule.useTemplatePreparation(findActiveTemplate(userId, scheduleAddDto.getPreparationTemplateId()));
        } else if (hasCustom) {
            schedule.useCustomPreparation();
        } else {
            schedule.useDefaultPreparation();
        }
    }

    private void applyModifyPreparationMode(Schedule schedule, Long userId, ScheduleModDto scheduleModDto) {
        if (scheduleModDto.getPreparationMode() == null) {
            if (scheduleModDto.getPreparationTemplateId() != null
                    || scheduleModDto.getCustomPreparations() != null) {
                throw new GeneralException(INVALID_INPUT);
            }
            return;
        }

        switch (scheduleModDto.getPreparationMode()) {
            case DEFAULT -> {
                if (scheduleModDto.getPreparationTemplateId() != null
                        || scheduleModDto.getCustomPreparations() != null) {
                    throw new GeneralException(INVALID_INPUT);
                }
                preparationScheduleRepository.deleteBySchedule(schedule);
                preparationScheduleRepository.flush();
                schedule.useDefaultPreparation();
            }
            case TEMPLATE -> {
                if (scheduleModDto.getPreparationTemplateId() == null
                        || scheduleModDto.getCustomPreparations() != null) {
                    throw new GeneralException(INVALID_INPUT);
                }
                preparationScheduleRepository.deleteBySchedule(schedule);
                preparationScheduleRepository.flush();
                schedule.useTemplatePreparation(findActiveTemplate(userId, scheduleModDto.getPreparationTemplateId()));
            }
            case CUSTOM -> {
                if (scheduleModDto.getPreparationTemplateId() != null
                        || scheduleModDto.getCustomPreparations() == null
                        || scheduleModDto.getCustomPreparations().isEmpty()) {
                    throw new GeneralException(INVALID_INPUT);
                }
                schedule.useCustomPreparation();
                replaceSchedulePreparations(schedule, preparationStepService.normalizeOrdered(scheduleModDto.getCustomPreparations()));
            }
        }
    }

    private PreparationTemplate findActiveTemplate(Long userId, UUID templateId) {
        PreparationTemplate template = preparationTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new GeneralException(PREPARATION_TEMPLATE_NOT_FOUND));
        if (template.isDeleted()) {
            throw new GeneralException(PREPARATION_TEMPLATE_DELETED);
        }
        return template;
    }

    public void refreshNotStartedTemplateModeSchedules(UUID templateId) {
        scheduleRepository.findNotStartedTemplateModeSchedules(templateId)
                .forEach(this::refreshScheduleNotification);
    }

    public void refreshNotStartedDefaultModeSchedules(Long userId) {
        scheduleRepository.findNotStartedDefaultModeSchedules(userId)
                .forEach(this::refreshScheduleNotification);
    }

    private void refreshScheduleNotification(Schedule schedule) {
        LocalDateTime newNotificationTime = getNotificationTime(schedule, schedule.getUser());
        NotificationSchedule notification = resolveNotificationForRefresh(schedule, newNotificationTime);
        if (newNotificationTime.equals(notification.getNotificationTime())) {
            notificationService.cancelScheduledNotification(notification.getId());
            notification.markAsUnsent();
            notificationScheduleRepository.save(notification);
            notificationService.scheduleReminder(notification);
            return;
        }
        updateAndRescheduleNotification(newNotificationTime, notification);
    }

    private NotificationSchedule resolveNotificationForRefresh(Schedule schedule, LocalDateTime notificationTime) {
        List<NotificationSchedule> notifications = notificationScheduleRepository
                .findAllByScheduleScheduleIdOrderByIdAsc(schedule.getScheduleId());
        if (notifications.isEmpty()) {
            NotificationSchedule notification = NotificationSchedule.builder()
                    .notificationTime(notificationTime)
                    .isSent(false)
                    .schedule(schedule)
                    .build();
            return notificationScheduleRepository.save(notification);
        }

        NotificationSchedule notification = notifications.get(0);
        for (int i = 1; i < notifications.size(); i++) {
            NotificationSchedule duplicate = notifications.get(i);
            notificationService.cancelScheduledNotification(duplicate.getId());
            notificationScheduleRepository.delete(duplicate);
        }
        return notification;
    }

    private PreparationDto mapPreparationUserToDto(PreparationUser preparationUser) {
        return new PreparationDto(
                preparationUser.getPreparationUserId(),
                preparationUser.getPreparationName(),
                defaultNonNegative(preparationUser.getPreparationTime()),
                preparationUser.getNextPreparation() != null
                        ? preparationUser.getNextPreparation().getPreparationUserId()
                        : null
        );
    }

    private Integer getEffectiveSpareTime(Schedule schedule) {
        return getEffectiveSpareTime(schedule, null);
    }

    private Integer getEffectiveSpareTime(Schedule schedule, Integer userSpareTime) {
        if (schedule.getScheduleSpareTime() != null) {
            return defaultNonNegative(schedule.getScheduleSpareTime());
        }
        return defaultNonNegative(userSpareTime != null ? userSpareTime : schedule.getUser().getSpareTime());
    }

    private Integer defaultNonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private Instant nowForPersistence() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private record AlarmWindowPreparationLookup(
            List<PreparationDto> defaultPreparations,
            Map<UUID, List<PreparationDto>> preparationsByScheduleId,
            Map<UUID, List<PreparationDto>> preparationsByTemplateId
    ) {
        private List<PreparationDto> resolve(Schedule schedule) {
            if (schedule.getStartedAt() != null || schedule.effectivePreparationMode() == PreparationMode.CUSTOM) {
                return preparationsByScheduleId.getOrDefault(schedule.getScheduleId(), List.of());
            }
            if (schedule.effectivePreparationMode() == PreparationMode.TEMPLATE) {
                PreparationTemplate template = schedule.getPreparationTemplate();
                if (template == null) {
                    throw new GeneralException(PREPARATION_TEMPLATE_NOT_FOUND);
                }
                return preparationsByTemplateId.getOrDefault(template.getPreparationTemplateId(), List.of());
            }
            return defaultPreparations;
        }
    }

}
