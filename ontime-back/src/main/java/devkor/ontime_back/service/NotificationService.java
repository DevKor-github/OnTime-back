package devkor.ontime_back.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import devkor.ontime_back.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final UserSettingRepository userSettingRepository;
    private final TaskScheduler taskScheduler;
    private final NotificationScheduleRepository notificationScheduleRepository;

    public void scheduleReminder(NotificationSchedule notificationSchedule) {
        LocalDateTime reminderTime = notificationSchedule.getNotificationTime();

        if (reminderTime.isBefore(LocalDateTime.now())) {
            log.warn("약속 알림 시간이 과거인 경우 알림 스케줄링하지 않습니다. {} ({})", notificationSchedule.getSchedule().getScheduleName(), reminderTime);
            return;
        }

        taskScheduler.schedule(
                () -> {
                    sendReminder(notificationSchedule, "약속 5분 전 입니다");
                },
                Date.from(reminderTime.atZone(ZoneId.systemDefault()).toInstant())
        );
        log.info("스케줄 등록 완료 {} ({})", notificationSchedule.getSchedule().getScheduleName(), reminderTime);
    }

    @Transactional
    public void sendReminder(NotificationSchedule notificationSchedule, String message) {
        Long userId = notificationSchedule.getSchedule().getUser().getId();

        if (userId != null) {
            UserSetting userSetting = userSettingRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("No UserSetting found in schedule's user"));// Repository 메서드 가정

            if (Boolean.TRUE.equals(userSetting.getIsNotificationsEnabled())) {
                sendNotificationToUser(notificationSchedule.getSchedule(), message);
                notificationSchedule.changeStatusToSent();
                notificationScheduleRepository.save(notificationSchedule);
            }
        }
    }

    public void sendReminder(List<Schedule> schedules, String message) {
        for (Schedule schedule : schedules) {
            User user = schedule.getUser();
            Long userId = user.getId();

            if (userId != null) {
                UserSetting userSetting = userSettingRepository.findByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException("No UserSetting found in schedule's user"));// Repository 메서드 가정

                if (userSetting != null && userSetting.getIsNotificationsEnabled()) {
                    sendNotificationToUser(schedule, message);
                }
            }
        }
    }

    @Transactional
    public void sendNotificationToUser(Schedule schedule, String message) {
        User user = schedule.getUser();
        String firebaseToken = user.getFirebaseToken();

        Message firebaseMessage = Message.builder()
                .putData("title", "약속 알림")
                .putData("content", user.getName() + "님 " + message + "\n약속명: " + schedule.getScheduleName())
                .setToken(firebaseToken)
                .build();

        try {
            FirebaseMessaging.getInstance().send(firebaseMessage);
            log.info("Firebase에 성공적으로 push notification 요청을 보냈으며, Firebase로부터 적절한 응답을 받았습니다 \n알림 푸시한 약속:" + schedule.getScheduleName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}