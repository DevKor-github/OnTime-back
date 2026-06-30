package devkor.ontime_back.service;

import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationDeliveryService notificationDeliveryService;

    @Async("notificationAsyncExecutor")
    @Transactional
    public void dispatchReminder(Long notificationId, String message) {
        NotificationSchedule notificationSchedule = notificationScheduleRepository.findByIdWithScheduleAndUser(notificationId)
                .orElse(null);
        if (notificationSchedule == null) {
            log.warn("예약된 알림을 찾을 수 없어 푸시 알림을 건너뜁니다. notificationId={}", notificationId);
            return;
        }

        notificationDeliveryService.sendReminder(notificationSchedule, message);
    }
}
