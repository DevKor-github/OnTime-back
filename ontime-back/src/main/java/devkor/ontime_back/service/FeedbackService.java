package devkor.ontime_back.service;

import devkor.ontime_back.dto.FeedbackAddDto;
import devkor.ontime_back.entity.Feedback;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.FeedbackRepository;
import devkor.ontime_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;

    @Transactional
    public void saveFeedback(Long userId, FeedbackAddDto feedbackAddDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime currentTime = LocalDateTime.now();

        Feedback feedback = Feedback.builder()
                .feedbackId(feedbackAddDto.getFeedbackId())
                .user(user)
                .message(feedbackAddDto.getMessage())
                .createAt(currentTime)
                .build();

        feedbackRepository.save(feedback);

    }
}
