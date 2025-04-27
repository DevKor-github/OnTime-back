package devkor.ontime_back.scheduler;

import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Transactional(readOnly = true)
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final ScheduleRepository scheduleRepository;

    public NotificationScheduler(NotificationService notificationService, ScheduleRepository scheduleRepository) {
        this.notificationService = notificationService;
        this.scheduleRepository = scheduleRepository;
    }

    // 매일 밤 9시, 다음 날 약속이 있는 사용자에게 알림 전송
    @Scheduled(cron = "0 0 21 * * *")
    //@Scheduled(fixedRate = 10000) // 테스트용 애너테이션(아래 스케줄러가 10초마다 실행됨)
    public void sendEveningReminder() {
        LocalDateTime startOfTomorrow = LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime endOfTomorrow = startOfTomorrow.with(LocalTime.MAX);

        List<Schedule> schedulesForTomorrow = scheduleRepository.findSchedulesBetween(startOfTomorrow, endOfTomorrow);
        notificationService.sendReminder(schedulesForTomorrow, "내일 예정된 약속이 있습니다.");
    }

    // 매일 아침 8시, 당일 약속이 있는 사용자에게 알림 전송
    @Scheduled(cron = "0 0 8 * * *")
    //@Scheduled(fixedRate = 10000) // 테스트용 애너테이션(아래 스케줄러가 10초마다 실행됨)
    public void sendMorningReminder() {
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.with(LocalTime.MAX);

        List<Schedule> schedulesForToday = scheduleRepository.findSchedulesBetween(startOfToday, endOfToday);
        notificationService.sendReminder(schedulesForToday, "오늘 예정된 약속이 있습니다.");
    }
}

