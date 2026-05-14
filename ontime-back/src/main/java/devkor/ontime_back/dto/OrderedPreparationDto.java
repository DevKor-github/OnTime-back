package devkor.ontime_back.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class OrderedPreparationDto {
    @NotNull(message = "preparationId는 필수입니다.")
    private UUID preparationId;

    @NotBlank(message = "준비과정 이름은 필수입니다.")
    @Size(max = 50, message = "준비과정 이름은 50자 이하여야 합니다.")
    private String preparationName;

    @NotNull(message = "준비 시간은 필수입니다.")
    @Min(value = 1, message = "준비 시간은 1 이상이어야 합니다.")
    @Max(value = 1440, message = "준비 시간은 1440 이하여야 합니다.")
    private Integer preparationTime;

    @NotNull(message = "orderIndex는 필수입니다.")
    @Min(value = 0, message = "orderIndex는 0 이상이어야 합니다.")
    private Integer orderIndex;
}
