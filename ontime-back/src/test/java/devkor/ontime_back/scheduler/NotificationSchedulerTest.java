package devkor.ontime_back.scheduler;

import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ScheduleRepository scheduleRepository;

    private NotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationScheduler(notificationService, scheduleRepository);
    }

    @Test
    void sendEveningReminderLooksUpTomorrowSchedulesAndSendsTomorrowMessage() {
        Schedule schedule = Schedule.builder().scheduleName("Tomorrow meeting").build();
        when(scheduleRepository.findSchedulesBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(schedule));

        scheduler.sendEveningReminder();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(scheduleRepository).findSchedulesBetween(startCaptor.capture(), endCaptor.capture());
        assertThat(startCaptor.getValue().toLocalDate()).isEqualTo(LocalDateTime.now().plusDays(1).toLocalDate());
        assertThat(startCaptor.getValue().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(endCaptor.getValue().toLocalTime()).isEqualTo(LocalTime.MAX);
        verify(notificationService).sendReminder(List.of(schedule), "내일 예정된 약속이 있습니다.");
    }

    @Test
    void sendMorningReminderLooksUpTodaySchedulesAndSendsTodayMessage() {
        Schedule schedule = Schedule.builder().scheduleName("Today meeting").build();
        when(scheduleRepository.findSchedulesBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(schedule));

        scheduler.sendMorningReminder();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(scheduleRepository).findSchedulesBetween(startCaptor.capture(), endCaptor.capture());
        assertThat(startCaptor.getValue().toLocalDate()).isEqualTo(LocalDateTime.now().toLocalDate());
        assertThat(startCaptor.getValue().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(endCaptor.getValue().toLocalTime()).isEqualTo(LocalTime.MAX);
        verify(notificationService).sendReminder(List.of(schedule), "오늘 예정된 약속이 있습니다.");
    }
}
