package devkor.ontime_back.dto;

import lombok.Getter;
import java.util.UUID;

@Getter
public class FeedbackAddDto {
    private UUID feedbackId;
    private String message;
}
