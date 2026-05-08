package devkor.ontime_back.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStatusFailureDto {
    @Size(max = 36, message = "scheduleId는 36자 이하여야 합니다.")
    private String scheduleId;
    @Pattern(regexp = "preparationLoadFailed|scheduleInvalid|platformError|unknown", message = "failure reason 값이 올바르지 않습니다.")
    private String reason;
}
