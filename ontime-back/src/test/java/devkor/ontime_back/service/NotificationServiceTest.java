package devkor.ontime_back.service;

import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                taskScheduler,
                notificationDispatchService,
                notificationDeliveryService
        );
    }

    @Test
    void scheduleReminderIgnoresPastReminderTimes() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now().minusMinutes(1));

        notificationService.scheduleReminder(notification);

        verifyNoInteractions(taskScheduler, notificationDispatchService);
    }

    @Test
    void scheduleReminderRequiresPersistedNotificationId() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> notificationService.scheduleReminder(notification))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("NotificationSchedule must be persisted before scheduling");

        verifyNoInteractions(taskScheduler, notificationDispatchService);
    }

    @Test
    void scheduleReminderRegistersFutureTaskWithNotificationIdAndCanCancelIt() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(notification, "id", 10L);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Date.class));
        when(future.isCancelled()).thenReturn(false);

        notificationService.scheduleReminder(notification);
        notificationService.cancelScheduledNotification(10L);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Date.class));
        verify(future).cancel(true);

        runnableCaptor.getValue().run();

        verify(notificationDispatchService).dispatchReminder(
                10L,
                "준비 시작해야 합니다.(현재 시각: 약속시각 - (여유시간 + 이동시간 + 총준비시간) )"
        );
    }

    @Test
    void sendReminderDelegatesSingleNotificationDelivery() {
        NotificationSchedule notification = notificationSchedule(LocalDateTime.now());

        notificationService.sendReminder(notification, "message");

        verify(notificationDeliveryService).sendReminder(notification, "message");
    }

    @Test
    void sendReminderDelegatesListNotificationDelivery() {
        Schedule schedule = Schedule.builder()
                .scheduleId(UUID.randomUUID())
                .scheduleName("Meeting")
                .user(User.builder().id(1L).build())
                .build();

        notificationService.sendReminder(List.of(schedule), "message");

        verify(notificationDeliveryService).sendReminder(List.of(schedule), "message");
    }

    @Test
    void sendNotificationToUserDelegatesDelivery() {
        Schedule schedule = Schedule.builder()
                .scheduleId(UUID.randomUUID())
                .scheduleName("Meeting")
                .user(User.builder().id(1L).build())
                .build();

        notificationService.sendNotificationToUser(schedule, "message");

        verify(notificationDeliveryService).sendNotificationToUser(schedule, "message");
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
                                .build())
                        .build())
                .build();
    }
}
