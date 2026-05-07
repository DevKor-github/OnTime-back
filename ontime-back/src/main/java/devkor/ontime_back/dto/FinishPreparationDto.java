package devkor.ontime_back.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
public class FinishPreparationDto {
    private UUID scheduleId;
    private Integer latenessTime;
    @Builder
    public FinishPreparationDto(UUID scheduleId, Integer latenessTime) {
        this.scheduleId = scheduleId;
        this.latenessTime = latenessTime;
    }
}
