package devkor.ontime_back.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class FinishPreparationDto {
    private UUID scheduleId;
    @NotNull(message = "지각 시간은 필수입니다.")
    @Min(value = 0, message = "지각 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "지각 시간은 1440 이하여야 합니다.")
    private Integer latenessTime;
    @Builder
    public FinishPreparationDto(UUID scheduleId, Integer latenessTime) {
        this.scheduleId = scheduleId;
        this.latenessTime = latenessTime;
    }
}
