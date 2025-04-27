package devkor.ontime_back.service;

import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRecoveryService {

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationService notificationService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverNotificationSchedules() {
        log.info("서버 부팅 완료: 알림 스케줄 복구 시작");

        LocalDateTime now = LocalDateTime.now();
        List<NotificationSchedule> pendingNotifications = notificationScheduleRepository.findAllWithScheduleAndUser(now);


        for (NotificationSchedule notification : pendingNotifications) {
            notificationService.scheduleReminder(notification);
        }

        log.info("알림 스케줄 복구 완료: 복구된 알림 수 = {}", pendingNotifications.size());
    }
}