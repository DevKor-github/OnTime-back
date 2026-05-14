package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PreparationTemplateResponseDto {
    private UUID templateId;
    private String templateName;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private List<OrderedPreparationDto> preparations;
}
