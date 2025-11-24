package devkor.ontime_back.service;

import devkor.ontime_back.dto.UpdateSpareTimeDto;
import devkor.ontime_back.dto.UserOnboardingDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserAuthService userAuthService;
    private final PreparationUserService preparationUserService;

    // 성실도 점수 반환
    public Float getPunctualityScore(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."))
                .getPunctualityScore();
    }

    // 성실도 점수 초기화
    @Transactional
    public Float resetPunctualityScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));
        user.resetPunctualityScore(); // 점수 초기화
        userRepository.save(user);

        return user.getPunctualityScore();
    }

    // 성실도 점수 업데이트
    @Transactional
    public User updatePunctualityScore(Long userId, Integer latenessTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));

        if ( user.getPunctualityScore() == null || user.getPunctualityScore() == (float) -1) {
            // 초기화 이후 첫 약속
            user.setScheduleCountAfterReset(1);
            user.setLatenessCountAfterReset(latenessTime > 0 ? 1 : 0);
        } else {
            // 기존 성실도 점수가 존재 -> 약속 수와 지각 수 업데이트
            user.setScheduleCountAfterReset(user.getScheduleCountAfterReset() + 1);
            if (latenessTime > 0) {
                user.setLatenessCountAfterReset(user.getLatenessCountAfterReset() + 1);
            }
        }

        // 성실도 점수 계산
        int totalSchedules = user.getScheduleCountAfterReset();
        int lateSchedules = user.getLatenessCountAfterReset();
        float punctualityScore = (1 - ((float) lateSchedules / totalSchedules)) * 100;

        user.setPunctualityScore(punctualityScore);
        userRepository.save(user);

        return user;
    }

    // 여유시간 업데이트
    @Transactional
    public User updateSpareTime(Long userId, UpdateSpareTimeDto updateSpareTimeDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));

        user.updateSpareTime(updateSpareTimeDto.getNewSpareTime());

        userRepository.save(user);

        return user;
    }

    @Transactional
    public void onboarding(Long userId, UserOnboardingDto userOnboardingDto) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));

        user.updateAdditionalInfo(userOnboardingDto.getSpareTime(), userOnboardingDto.getNote());
        userRepository.save(user);
        preparationUserService.setFirstPreparationUser(userId, userOnboardingDto.getPreparationList());
        user.authorizeUser();
        userRepository.save(user);
    }

    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 id입니다."));
    }
}
