package devkor.ontime_back.service;

import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import devkor.ontime_back.repository.UserSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private AlarmService alarmService;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                userSettingRepository,
                alarmService,
                taskScheduler,
                notificationScheduleRepository
        );
    }

    @Test
    void scheduleReminderIgnoresPastReminderTimes() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now().minusMinutes(1));

        notificationService.scheduleReminder(notification);

        verifyNoInteractions(taskScheduler);
    }

    @Test
    void scheduleReminderRegistersFutureTaskAndCanCancelIt() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(notification, "id", 10L);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Date.class));
        when(future.isCancelled()).thenReturn(false);

        notificationService.scheduleReminder(notification);
        notificationService.cancelScheduledNotification(10L);

        verify(taskScheduler).schedule(any(Runnable.class), any(Date.class));
        verify(future).cancel(true);
    }

    @Test
    void sendReminderDoesNothingWhenUserIdIsMissing() {
        NotificationSchedule notification = NotificationSchedule.builder()
                .notificationTime(LocalDateTime.now())
                .isSent(false)
                .schedule(Schedule.builder()
                        .scheduleId(UUID.randomUUID())
                        .scheduleName("No owner")
                        .user(User.builder().build())
                        .build())
                .build();

        notificationService.sendReminder(notification, "message");

        verifyNoInteractions(userSettingRepository, alarmService, notificationScheduleRepository);
        assertThat(notification.getIsSent()).isFalse();
    }

    @Test
    void sendReminderDoesNotSendWhenUserDisabledNotifications() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now());
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(userSetting(false)));

        notificationService.sendReminder(notification, "message");

        verifyNoInteractions(alarmService, notificationScheduleRepository);
        assertThat(notification.getIsSent()).isFalse();
    }

    @Test
    void sendReminderDoesNotSendWhenNativeAlarmAlreadyCoversSchedule() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now());
        UUID scheduleId = notification.getSchedule().getScheduleId();
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(userSetting(true)));
        when(alarmService.shouldSuppressLegacyReminder(1L, scheduleId, notification.getNotificationTime()))
                .thenReturn(true);

        notificationService.sendReminder(notification, "message");

        verify(notificationScheduleRepository, never()).save(notification);
        assertThat(notification.getIsSent()).isFalse();
    }

    @Test
    void sendReminderMarksNotificationSentWhenUserEnabledNotificationsAndNativeAlarmDoesNotCoverIt() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now());
        UUID scheduleId = notification.getSchedule().getScheduleId();
        NotificationService spyService = spy(notificationService);
        doNothing().when(spyService).sendNotificationToUser(notification.getSchedule(), "message");
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(userSetting(true)));
        when(alarmService.shouldSuppressLegacyReminder(1L, scheduleId, notification.getNotificationTime()))
                .thenReturn(false);

        spyService.sendReminder(notification, "message");

        verify(spyService).sendNotificationToUser(notification.getSchedule(), "message");
        verify(notificationScheduleRepository).save(notification);
        assertThat(notification.getIsSent()).isTrue();
    }

    @Test
    void sendReminderFailsClearlyWhenUserSettingsAreMissing() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now());
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.sendReminder(notification, "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No UserSetting found in schedule's user");
    }

    @Test
    void listReminderSkipsSchedulesWithoutPersistedUsers() {
        Schedule schedule = Schedule.builder()
                .scheduleId(UUID.randomUUID())
                .scheduleName("Unsaved user")
                .user(User.builder().build())
                .build();

        notificationService.sendReminder(List.of(schedule), "message");

        verifyNoInteractions(userSettingRepository, alarmService, notificationScheduleRepository);
    }

    @Test
    void listReminderSendsOnlyForSchedulesWhoseUsersAllowNotifications() {
        Schedule enabledSchedule = scheduleForListReminder(1L, "Enabled");
        Schedule disabledSchedule = scheduleForListReminder(2L, "Disabled");
        NotificationService spyService = spy(notificationService);
        doNothing().when(spyService).sendNotificationToUser(any(Schedule.class), eq("message"));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(userSetting(true)));
        when(userSettingRepository.findByUserId(2L)).thenReturn(Optional.of(userSetting(false)));

        spyService.sendReminder(List.of(enabledSchedule, disabledSchedule), "message");

        verify(spyService).sendNotificationToUser(enabledSchedule, "message");
        verify(spyService, never()).sendNotificationToUser(disabledSchedule, "message");
    }

    @Test
    void listReminderFailsClearlyWhenPersistedUserHasNoSettings() {
        Schedule schedule = scheduleForListReminder(1L, "Missing setting");
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.sendReminder(List.of(schedule), "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No UserSetting found in schedule's user");
    }

    @Test
    void sendNotificationToUserDoesNotPropagateFirebaseClientFailures() {
        Schedule schedule = scheduleForListReminder(1L, "Firebase smoke test");

        notificationService.sendNotificationToUser(schedule, "message");

        assertThat(schedule.getUser().getFirebaseToken()).isEqualTo("firebase-token-1");
    }

    private NotificationSchedule notificationSchedule(LocalDateTime notificationTime) {
        return NotificationSchedule.builder()
                .notificationTime(notificationTime)
                .isSent(false)
                .schedule(Schedule.builder()
                        .scheduleId(UUID.randomUUID())
                        .scheduleName("Morning meeting")
                        .user(User.builder()
                                .id(1L)
                                .name("User")
                                .firebaseToken("firebase-token")
                                .role(Role.USER)
                                .build())
                        .build())
                .build();
    }

    private UserSetting userSetting(boolean notificationsEnabled) {
        return UserSetting.builder()
                .userSettingId(UUID.randomUUID())
                .isNotificationsEnabled(notificationsEnabled)
                .build();
    }

    private Schedule scheduleForListReminder(Long userId, String scheduleName) {
        return Schedule.builder()
                .scheduleId(UUID.randomUUID())
                .scheduleName(scheduleName)
                .user(User.builder()
                        .id(userId)
                        .name("User " + userId)
                        .firebaseToken("firebase-token-" + userId)
                        .role(Role.USER)
                        .build())
                .build();
    }
}
