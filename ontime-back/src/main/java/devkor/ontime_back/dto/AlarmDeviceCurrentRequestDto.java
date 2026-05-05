package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDeviceCurrentRequestDto {
    private String deviceId;
    private String platform;
    private String appVersion;
    private String osVersion;
    private Boolean supportsNativeAlarm;
    private String nativeAlarmProvider;
    private String fallbackProvider;
}
