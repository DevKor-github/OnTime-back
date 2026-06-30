package devkor.ontime_back.service;

import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TaskScheduler taskScheduler;
    private final NotificationDispatchService notificationDispatchService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleReminder(NotificationSchedule notificationSchedule) {
        LocalDateTime reminderTime = notificationSchedule.getNotificationTime();

        if (reminderTime.isBefore(LocalDateTime.now())) {
            log.warn("약속 알림 시간이 과거인 경우 알림 스케줄링하지 않습니다. {} ({})", notificationSchedule.getSchedule().getScheduleName(), reminderTime);
            return;
        }

        Long notificationId = notificationSchedule.getId();
        if (notificationId == null) {
            throw new IllegalArgumentException("NotificationSchedule must be persisted before scheduling");
        }

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> notificationDispatchService.dispatchReminder(
                        notificationId,
                        "준비 시작해야 합니다.(현재 시각: 약속시각 - (여유시간 + 이동시간 + 총준비시간) )"),
                Date.from(reminderTime.atZone(ZoneId.systemDefault()).toInstant())
        );

        scheduledTasks.put(notificationId, future);

        log.info("스케줄 등록 완료 {} ({})", notificationSchedule.getSchedule().getScheduleName(), reminderTime);
    }

    public void cancelScheduledNotification(Long notificationId) {
        ScheduledFuture<?> future = scheduledTasks.get(notificationId);
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
            scheduledTasks.remove(notificationId);
            log.info("스케줄 취소 완료: notificationId={}", notificationId);
        }
    }

    public void sendReminder(NotificationSchedule notificationSchedule, String message) {
        notificationDeliveryService.sendReminder(notificationSchedule, message);
    }

    public void sendReminder(List<Schedule> schedules, String message) {
        notificationDeliveryService.sendReminder(schedules, message);
    }

    public void sendNotificationToUser(Schedule schedule, String message) {
        notificationDeliveryService.sendNotificationToUser(schedule, message);
    }
}
