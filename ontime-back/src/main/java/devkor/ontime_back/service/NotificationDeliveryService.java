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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDeliveryService {

    private final UserSettingRepository userSettingRepository;
    private final AlarmService alarmService;
    private final NotificationScheduleRepository notificationScheduleRepository;

    @Transactional
    public void sendReminder(NotificationSchedule notificationSchedule, String message) {
        Long userId = notificationSchedule.getSchedule().getUser().getId();

        if (userId != null) {
            UserSetting userSetting = userSettingRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("No UserSetting found in schedule's user"));
            log.debug("사용자 알림 전송 설정 여부: " + userSetting.getIsNotificationsEnabled());

            if (Boolean.TRUE.equals(userSetting.getIsNotificationsEnabled())) {
                if (alarmService.shouldSuppressLegacyReminder(
                        userId,
                        notificationSchedule.getSchedule().getScheduleId(),
                        notificationSchedule.getNotificationTime())) {
                    log.info("현재 기기 로컬 알람 커버리지로 인해 레거시 푸시 알림을 생략합니다. scheduleId={}",
                            notificationSchedule.getSchedule().getScheduleId());
                    return;
                }
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
                        .orElseThrow(() -> new IllegalArgumentException("No UserSetting found in schedule's user"));

                if (userSetting != null && userSetting.getIsNotificationsEnabled()) {
                    sendNotificationToUser(schedule, message);
                }
            }
        }
    }

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
