package devkor.ontime_back.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDeviceUnregisterRequestDto {
    @Pattern(regexp = "^[A-Za-z0-9._:-]{16,128}$", message = "deviceId 형식이 올바르지 않습니다.")
    private String deviceId;
}
