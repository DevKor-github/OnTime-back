package devkor.ontime_back.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import devkor.ontime_back.dto.FirebaseTokenAddDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FirebaseTokenService {
    private final UserRepository userRepository;

    @Transactional
    public void registerFirebaseToken(Long userId, FirebaseTokenAddDto firebaseTokenAddDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.updateFirebaseToken(firebaseTokenAddDto.getFirebaseToken());
        userRepository.save(user);
    }

    public void sendTestNotification(Long userId) {
        String firebaseToken = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("FirebaseToken not found"))
                .getFirebaseToken();

        Message firebaseMessage = Message.builder()
                .putData("title", "약속 알림")
                .putData("content", "파이어베이스 알림 테스트입니다~~!")
                .setToken(firebaseToken)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(firebaseMessage);
            System.out.println("Successfully sent message: " + response);

        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneralException(ErrorCode.FIREBASE);
        }
    }
}
