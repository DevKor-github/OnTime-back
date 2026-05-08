package devkor.ontime_back.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateSpareTimeDto {
    @NotNull(message = "여유 시간은 필수입니다.")
    @Min(value = 0, message = "여유 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "여유 시간은 1440 이하여야 합니다.")
    private Integer newSpareTime;
    @Builder
    public UpdateSpareTimeDto(Integer newSpareTime) {
        this.newSpareTime = newSpareTime;
    }
}
