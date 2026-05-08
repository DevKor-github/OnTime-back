package devkor.ontime_back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStatusReportRequestDto {
    @NotBlank(message = "deviceId는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9._:-]{16,128}$", message = "deviceId 형식이 올바르지 않습니다.")
    private String deviceId;
    @NotNull(message = "reconciledAt은 필수입니다.")
    private OffsetDateTime reconciledAt;
    @NotNull(message = "scheduleWindowStart는 필수입니다.")
    private LocalDateTime scheduleWindowStart;
    @NotNull(message = "scheduleWindowEnd는 필수입니다.")
    private LocalDateTime scheduleWindowEnd;
    @NotNull(message = "alarmCoverageStart는 필수입니다.")
    private LocalDateTime alarmCoverageStart;
    @NotNull(message = "alarmCoverageEnd는 필수입니다.")
    private LocalDateTime alarmCoverageEnd;
    @NotBlank(message = "status는 필수입니다.")
    @Pattern(regexp = "armed|partial|disabled|permissionNeeded|unsupported|settingsUnavailable", message = "status 값이 올바르지 않습니다.")
    private String status;
    @Pattern(regexp = "nativePermissionDenied|notificationPermissionDenied", message = "permissionIssue 값이 올바르지 않습니다.")
    private String permissionIssue;
    @NotBlank(message = "nativeAlarmProvider는 필수입니다.")
    @Pattern(regexp = "androidAlarmManager|iosAlarmKit|none", message = "nativeAlarmProvider 값이 올바르지 않습니다.")
    private String nativeAlarmProvider;
    @NotBlank(message = "fallbackProvider는 필수입니다.")
    @Pattern(regexp = "localNotification|none", message = "fallbackProvider 값이 올바르지 않습니다.")
    private String fallbackProvider;
    @Min(value = 0, message = "armedScheduleCount는 0 이상이어야 합니다.")
    @Max(value = 1440, message = "armedScheduleCount는 1440 이하여야 합니다.")
    private Integer armedScheduleCount;
    private List<String> armedScheduleIds;
    @Min(value = 0, message = "skippedScheduleCount는 0 이상이어야 합니다.")
    @Max(value = 1440, message = "skippedScheduleCount는 1440 이하여야 합니다.")
    private Integer skippedScheduleCount;
    private List<@Valid AlarmStatusFailureDto> failures;

    @AssertTrue(message = "scheduleWindowEnd는 scheduleWindowStart 이후여야 합니다.")
    public boolean isScheduleWindowRangeValid() {
        return scheduleWindowStart == null || scheduleWindowEnd == null || !scheduleWindowEnd.isBefore(scheduleWindowStart);
    }

    @AssertTrue(message = "alarmCoverageEnd는 alarmCoverageStart 이후여야 합니다.")
    public boolean isAlarmCoverageRangeValid() {
        return alarmCoverageStart == null || alarmCoverageEnd == null || !alarmCoverageEnd.isBefore(alarmCoverageStart);
    }
}
