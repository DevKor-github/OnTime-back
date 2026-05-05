package devkor.ontime_back.dto;

import lombok.*;

import java.util.UUID;

@ToString
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAddDto {
    private UUID feedbackId;
    private String message;
}
