package devkor.ontime_back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
public class UserOnboardingDto {
    @NotNull(message = "여유 시간은 필수입니다.")
    @Min(value = 0, message = "여유 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "여유 시간은 1440 이하여야 합니다.")
    private Integer spareTime;
    @Size(max = 1000, message = "주의사항은 1000자 이하여야 합니다.")
    private String note;
    @NotEmpty(message = "준비과정은 하나 이상 필요합니다.")
    private List<@Valid PreparationDto> preparationList;

    public UserOnboardingDto(Integer spareTime, String note, List<PreparationDto> preparationList) {
        this.spareTime = spareTime;
        this.note = note;
        this.preparationList = preparationList;
    }
}
