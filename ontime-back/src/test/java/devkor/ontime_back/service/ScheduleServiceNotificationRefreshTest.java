package devkor.ontime_back.service;

import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.PreparationMode;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import devkor.ontime_back.repository.PlaceRepository;
import devkor.ontime_back.repository.PreparationScheduleRepository;
import devkor.ontime_back.repository.PreparationTemplateRepository;
import devkor.ontime_back.repository.PreparationTemplateStepRepository;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceNotificationRefreshTest {

    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AlarmService alarmService;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PreparationScheduleRepository preparationScheduleRepository;
    @Mock
    private PreparationUserRepository preparationUserRepository;
    @Mock
    private PreparationTemplateRepository preparationTemplateRepository;
    @Mock
    private PreparationTemplateStepRepository preparationTemplateStepRepository;
    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;
    @Mock
    private PreparationStepService preparationStepService;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                userService,
                notificationService,
                alarmService,
                scheduleRepository,
                userRepository,
                placeRepository,
                preparationScheduleRepository,
                preparationUserRepository,
                preparationTemplateRepository,
                preparationTemplateStepRepository,
                notificationScheduleRepository,
                preparationStepService
        );
    }

    @Test
    void refreshDefaultModeScheduleDeduplicatesNotificationRows() {
        UUID scheduleId = UUID.randomUUID();
        User user = User.builder()
                .id(1L)
                .spareTime(10)
                .build();
        Schedule schedule = Schedule.builder()
                .scheduleId(scheduleId)
                .scheduleName("학교")
                .scheduleTime(LocalDateTime.of(2026, 6, 29, 14, 30))
                .moveTime(10)
                .doneStatus(DoneStatus.NOT_ENDED)
                .preparationMode(PreparationMode.DEFAULT)
                .user(user)
                .build();
        NotificationSchedule canonical = notification(1L, schedule, LocalDateTime.of(2026, 6, 29, 13, 0));
        NotificationSchedule duplicate1 = notification(2L, schedule, LocalDateTime.of(2026, 6, 29, 13, 5));
        NotificationSchedule duplicate2 = notification(3L, schedule, LocalDateTime.of(2026, 6, 29, 13, 10));
        NotificationSchedule duplicate3 = notification(4L, schedule, LocalDateTime.of(2026, 6, 29, 13, 15));

        when(scheduleRepository.findNotStartedDefaultModeSchedules(1L)).thenReturn(List.of(schedule));
        when(scheduleRepository.findByIdWithUser(scheduleId)).thenReturn(Optional.of(schedule));
        when(preparationUserRepository.findByUserIdWithNextPreparation(1L)).thenReturn(List.of());
        when(preparationStepService.toLinkedDtoFromUser(List.of())).thenReturn(List.of(
                new PreparationDto(UUID.randomUUID(), "메이크업", 38, null),
                new PreparationDto(UUID.randomUUID(), "화장실 가기", 3, null)
        ));
        when(alarmService.getDefaultAlarmOffsetMinutes(1L)).thenReturn(0);
        when(notificationScheduleRepository.findAllByScheduleScheduleIdOrderByIdAsc(scheduleId))
                .thenReturn(List.of(canonical, duplicate1, duplicate2, duplicate3));

        scheduleService.refreshNotStartedDefaultModeSchedules(1L);

        assertThat(canonical.getNotificationTime()).isEqualTo(LocalDateTime.of(2026, 6, 29, 13, 29));
        verify(notificationService).cancelScheduledNotification(2L);
        verify(notificationService).cancelScheduledNotification(3L);
        verify(notificationService).cancelScheduledNotification(4L);
        verify(notificationScheduleRepository).delete(duplicate1);
        verify(notificationScheduleRepository).delete(duplicate2);
        verify(notificationScheduleRepository).delete(duplicate3);
        verify(notificationScheduleRepository).save(canonical);
        verify(notificationService).scheduleReminder(canonical);
    }

    private NotificationSchedule notification(Long id, Schedule schedule, LocalDateTime notificationTime) {
        NotificationSchedule notification = NotificationSchedule.builder()
                .schedule(schedule)
                .notificationTime(notificationTime)
                .isSent(false)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}
