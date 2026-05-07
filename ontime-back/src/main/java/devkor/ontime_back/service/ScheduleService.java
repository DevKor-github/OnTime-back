package devkor.ontime_back.service;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.entity.*;
import devkor.ontime_back.repository.*;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
    private final NotificationScheduleRepository notificationScheduleRepository;

    // scheduleId, userId를 통한 권한 확인
    private Schedule getScheduleWithAuthorization(UUID scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findByIdWithUser(scheduleId)
                .orElseThrow(() -> new GeneralException(SCHEDULE_NOT_FOUND));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(UNAUTHORIZED_ACCESS);
        }

        return schedule;
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
        getScheduleWithAuthorization(scheduleId, userId);
        NotificationSchedule notification = notificationScheduleRepository.findByScheduleScheduleId(scheduleId)
                .orElseThrow(() -> new GeneralException(NOTIFICATION_NOT_FOUND));
        scheduleRepository.deleteByScheduleId(scheduleId);
    }

    // schedule 수정
    @Transactional
    public void modifySchedule(Long userId, UUID scheduleId, ScheduleModDto scheduleModDto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        Place place = placeRepository.findByPlaceName(scheduleModDto.getPlaceName())
                .orElseGet(() -> placeRepository.save(new Place(scheduleModDto.getPlaceId(), scheduleModDto.getPlaceName())));

        schedule.updateSchedule(place, scheduleModDto);

        scheduleRepository.save(schedule);

        NotificationSchedule notification = notificationScheduleRepository.findByScheduleScheduleId(scheduleId)
                .orElseThrow(() -> new GeneralException(NOTIFICATION_NOT_FOUND));
        LocalDateTime newNotificationTime = getNotificationTime(schedule, user);
        updateAndRescheduleNotification(newNotificationTime, notification);
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
        Place place = placeRepository.findByPlaceName(scheduleAddDto.getPlaceName())
                .orElseGet(() -> placeRepository.save(new Place(scheduleAddDto.getPlaceId(), scheduleAddDto.getPlaceName())));

        Schedule schedule = scheduleAddDto.toEntity(user, place);
        scheduleRepository.save(schedule);

        LocalDateTime notificationTime = getNotificationTime(schedule, user);

        NotificationSchedule notification = NotificationSchedule.builder()
                .notificationTime(notificationTime)
                .isSent(false)
                .schedule(schedule)
                .build();
        notificationScheduleRepository.save(notification);

        notificationService.scheduleReminder(notification);
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
    public void updateLatenessTime(Schedule schedule, Integer latenessTime) {
        schedule.updateLatenessTime(latenessTime);
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

        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);
        boolean alreadyFinishedByDoneStatus = schedule.getDoneStatus() != null && schedule.getDoneStatus() != DoneStatus.NOT_ENDED;
        boolean alreadyFinishedByLatenessTime = schedule.getLatenessTime() != null && schedule.getLatenessTime() != -1;
        if (alreadyFinishedByDoneStatus || alreadyFinishedByLatenessTime) {
            throw new GeneralException(SCHEDULE_ALREADY_FINISHED);
        }

        updateLatenessTime(schedule, finishPreparationDto.getLatenessTime());
        userService.updatePunctualityScore(userId, finishPreparationDto.getLatenessTime());
    }

    // schedule에 따른 preparation 조회
    public List<PreparationDto> getPreparations(Long userId, UUID scheduleId) {
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        if (Boolean.TRUE.equals(schedule.getIsChange())) {
            return preparationScheduleRepository.findByScheduleWithNextPreparation(schedule).stream()
                    .map(preparationSchedule -> new PreparationDto(
                            preparationSchedule.getPreparationScheduleId(),
                            preparationSchedule.getPreparationName(),
                            defaultNonNegative(preparationSchedule.getPreparationTime()),
                            preparationSchedule.getNextPreparation() != null
                                    ? preparationSchedule.getNextPreparation().getPreparationScheduleId()
                                    : null
                    ))
                    .collect(Collectors.toList());
        } else {
            return preparationUserRepository.findByUserIdWithNextPreparation(schedule.getUser().getId()).stream()
                    .map(preparationUser -> new PreparationDto(
                            preparationUser.getPreparationUserId(),
                            preparationUser.getPreparationName(),
                            defaultNonNegative(preparationUser.getPreparationTime()),
                            preparationUser.getNextPreparation() != null
                                    ? preparationUser.getNextPreparation().getPreparationUserId()
                                    : null
                    ))
                    .collect(Collectors.toList());
        }
    }

    public List<AlarmWindowScheduleDto> getAlarmWindowSchedules(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new GeneralException(INVALID_INPUT);
        }
        if (Duration.between(startDate, endDate).compareTo(Duration.ofDays(14)) > 0) {
            throw new GeneralException(ALARM_WINDOW_RANGE_TOO_LONG);
        }

        List<Schedule> schedules = scheduleRepository.findAlarmWindowSchedules(userId, startDate, endDate, DoneStatus.NOT_ENDED);
        List<PreparationDto> userPreparations = preparationUserRepository.findByUserIdWithNextPreparation(userId).stream()
                .map(this::mapPreparationUserToDto)
                .collect(Collectors.toList());
        Integer defaultAlarmOffsetMinutes = alarmService.getDefaultAlarmOffsetMinutes(userId);

        return schedules.stream()
                .map(schedule -> mapToAlarmWindowDto(schedule, userPreparations, defaultAlarmOffsetMinutes))
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
                schedule.getDoneStatus()
        );
    }

    private AlarmWindowScheduleDto mapToAlarmWindowDto(Schedule schedule, List<PreparationDto> userPreparations, Integer defaultAlarmOffsetMinutes) {
        List<PreparationDto> preparations = Boolean.TRUE.equals(schedule.getIsChange())
                ? preparationScheduleRepository.findByScheduleWithNextPreparation(schedule).stream()
                .map(this::mapPreparationScheduleToDto)
                .collect(Collectors.toList())
                : userPreparations;

        int totalPreparationTime = preparations.stream()
                .map(PreparationDto::getPreparationTime)
                .map(this::defaultNonNegative)
                .reduce(0, Integer::sum);
        int moveTime = defaultNonNegative(schedule.getMoveTime());
        int scheduleSpareTime = getEffectiveSpareTime(schedule);

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
                .preparationStartTime(preparationStartTime)
                .defaultAlarmTime(defaultAlarmTime)
                .preparations(preparations)
                .alarmSettings(null)
                .build();
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
        if (schedule.getScheduleSpareTime() != null) {
            return defaultNonNegative(schedule.getScheduleSpareTime());
        }
        return defaultNonNegative(schedule.getUser().getSpareTime());
    }

    private Integer defaultNonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

}
