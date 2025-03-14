package devkor.ontime_back.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final UserSettingRepository userSettingRepository;

    public void sendReminder(List<Schedule> schedules, String message) {
        for (Schedule schedule : schedules) {
            User user = schedule.getUser();
            Long userId = user.getId();

            if (userId != null) {
                // UserSetting 테이블에서 해당 유저의 알림 설정 가져오기
                UserSetting userSetting = userSettingRepository.findByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException("No UserSetting found in schedule's user"));// Repository 메서드 가정

                // 알림 설정 확인
                if (userSetting != null && userSetting.getIsNotificationsEnabled()) {
                    sendNotificationToUser(schedule, message);
                }
            }
        }
    }

    private void sendNotificationToUser(Schedule schedule, String message) {
        User user = schedule.getUser();
        String firebaseToken = user.getFirebaseToken();

        Message firebaseMessage = Message.builder()
                .putData("title", "약속 알림")
                .putData("content", user.getName() + "님 " + message + "\n약속명: " + schedule.getScheduleName())
                .setToken(firebaseToken)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(firebaseMessage);
            System.out.println("Firebase에 성공적으로 push notification 요청을 보냈으며, Firebase로부터 적절한 응답을 받았습니다 \n응답 내용:" + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}