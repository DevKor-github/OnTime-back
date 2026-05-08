package devkor.ontime_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FirebaseTokenAddDto {
    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Size(max = 4096, message = "FCM 토큰은 4096자 이하여야 합니다.")
    String firebaseToken;
    @NotBlank(message = "deviceId는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9._:-]{16,128}$", message = "deviceId 형식이 올바르지 않습니다.")
    String deviceId;
}
