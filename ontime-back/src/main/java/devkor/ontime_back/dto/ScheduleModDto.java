package devkor.ontime_back.dto;

import devkor.ontime_back.entity.PreparationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleModDto {
    @NotNull(message = "placeId는 필수입니다.")
    private UUID placeId;
    @NotBlank(message = "장소 이름은 필수입니다.")
    @Size(max = 100, message = "장소 이름은 100자 이하여야 합니다.")
    private String placeName;
    @NotBlank(message = "일정 이름은 필수입니다.")
    @Size(max = 30, message = "일정 이름은 30자 이하여야 합니다.")
    private String scheduleName;
    @NotNull(message = "이동 시간은 필수입니다.")
    @Min(value = 0, message = "이동 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "이동 시간은 1440 이하여야 합니다.")
    private Integer moveTime;
    @NotNull(message = "일정 시간은 필수입니다.")
    private LocalDateTime scheduleTime;
    @Min(value = 0, message = "여유 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "여유 시간은 1440 이하여야 합니다.")
    private Integer scheduleSpareTime;
    @Min(value = 0, message = "지각 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "지각 시간은 1440 이하여야 합니다.")
    private Integer latenessTime;
    @Size(max = 1000, message = "일정 메모는 1000자 이하여야 합니다.")
    private String scheduleNote;
    private PreparationMode preparationMode;
    private UUID preparationTemplateId;
    private List<@Valid OrderedPreparationDto> customPreparations;
}
