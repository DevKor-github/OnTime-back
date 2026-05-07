package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class PreparationDto {
    private UUID preparationId;
    private String preparationName;
    private Integer preparationTime;
    private UUID nextPreparationId;
}
