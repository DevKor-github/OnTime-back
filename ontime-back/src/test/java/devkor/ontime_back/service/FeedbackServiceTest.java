package devkor.ontime_back.service;

import devkor.ontime_back.dto.FeedbackAddDto;
import devkor.ontime_back.entity.Feedback;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.FeedbackRepository;
import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(userRepository, feedbackRepository);
    }

    @Test
    void saveFeedbackPersistsFeedbackForTheCurrentUser() {
        User user = User.builder().id(1L).email("user@example.com").build();
        UUID feedbackId = UUID.randomUUID();
        FeedbackAddDto dto = FeedbackAddDto.builder()
                .feedbackId(feedbackId)
                .message("사용자 피드백")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        feedbackService.saveFeedback(1L, dto);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getFeedbackId()).isEqualTo(feedbackId);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getMessage()).isEqualTo("사용자 피드백");
        assertThat(captor.getValue().getCreateAt()).isNotNull();
    }

    @Test
    void saveFeedbackFailsWhenTheUserDoesNotExist() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.saveFeedback(
                404L,
                FeedbackAddDto.builder().feedbackId(UUID.randomUUID()).message("message").build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }
}
