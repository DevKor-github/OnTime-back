package devkor.ontime_back.service;

import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRecoveryServiceTest {

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    @Mock
    private NotificationService notificationService;

    private NotificationRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new NotificationRecoveryService(notificationScheduleRepository, notificationService);
    }

    @Test
    void recoverNotificationSchedulesReschedulesPendingNotificationsOnStartup() {
        NotificationSchedule first = notification("Morning meeting");
        NotificationSchedule second = notification("Evening meeting");
        when(notificationScheduleRepository.findAllWithScheduleAndUser(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(first, second));

        recoveryService.recoverNotificationSchedules();

        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationScheduleRepository).findAllWithScheduleAndUser(nowCaptor.capture());
        assertThat(nowCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
        verify(notificationService).scheduleReminder(first);
        verify(notificationService).scheduleReminder(second);
    }

    private NotificationSchedule notification(String name) {
        return NotificationSchedule.builder()
                .notificationTime(LocalDateTime.now().plusHours(1))
                .isSent(false)
                .schedule(Schedule.builder().scheduleName(name).build())
                .build();
    }
}
