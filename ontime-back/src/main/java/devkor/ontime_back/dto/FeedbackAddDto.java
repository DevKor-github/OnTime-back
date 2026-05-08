package devkor.ontime_back.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAddDto {
    private UUID feedbackId;
    @Size(max = 1000, message = "피드백은 1000자 이하여야 합니다.")
    private String message;
}
