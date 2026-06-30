package devkor.ontime_back.service;

import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    private NotificationDispatchService notificationDispatchService;

    @BeforeEach
    void setUp() {
        notificationDispatchService = new NotificationDispatchService(
                notificationScheduleRepository,
                notificationDeliveryService
        );
    }

    @Test
    void dispatchReminderReloadsNotificationByIdBeforeDelivery() {
        NotificationSchedule notification = notificationSchedule();
        when(notificationScheduleRepository.findByIdWithScheduleAndUser(10L))
                .thenReturn(Optional.of(notification));

        notificationDispatchService.dispatchReminder(10L, "message");

        verify(notificationScheduleRepository).findByIdWithScheduleAndUser(10L);
        verify(notificationDeliveryService).sendReminder(notification, "message");
    }

    @Test
    void dispatchReminderSkipsMissingNotification() {
        when(notificationScheduleRepository.findByIdWithScheduleAndUser(10L))
                .thenReturn(Optional.empty());

        notificationDispatchService.dispatchReminder(10L, "message");

        verify(notificationScheduleRepository).findByIdWithScheduleAndUser(10L);
        verifyNoInteractions(notificationDeliveryService);
    }

    private NotificationSchedule notificationSchedule() {
        return NotificationSchedule.builder()
                .notificationTime(LocalDateTime.now())
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
