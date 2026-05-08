package devkor.ontime_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AlarmDeviceCurrentRequestDto {
    @NotBlank(message = "deviceId는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9._:-]{16,128}$", message = "deviceId 형식이 올바르지 않습니다.")
    private String deviceId;
    @NotBlank(message = "platform은 필수입니다.")
    @Pattern(regexp = "android|ios", message = "platform은 android 또는 ios만 가능합니다.")
    private String platform;
    @Size(max = 128, message = "appVersion은 128자 이하여야 합니다.")
    private String appVersion;
    @Size(max = 128, message = "osVersion은 128자 이하여야 합니다.")
    private String osVersion;
    @NotNull(message = "supportsNativeAlarm은 필수입니다.")
    private Boolean supportsNativeAlarm;
    @NotBlank(message = "nativeAlarmProvider는 필수입니다.")
    @Pattern(regexp = "androidAlarmManager|iosAlarmKit|none", message = "nativeAlarmProvider 값이 올바르지 않습니다.")
    private String nativeAlarmProvider;
    @NotBlank(message = "fallbackProvider는 필수입니다.")
    @Pattern(regexp = "localNotification|none", message = "fallbackProvider 값이 올바르지 않습니다.")
    private String fallbackProvider;
}
