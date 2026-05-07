package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AlarmStatusCurrentResponseDto {
    private String deviceId;
    private Boolean active;
    private String platform;
    private String appVersion;
    private String osVersion;
    private Boolean supportsNativeAlarm;
    private String nativeAlarmProvider;
    private String fallbackProvider;
    private Instant lastSeenAt;
    private Instant reconciledAt;
    private LocalDateTime scheduleWindowStart;
    private LocalDateTime scheduleWindowEnd;
    private LocalDateTime alarmCoverageStart;
    private LocalDateTime alarmCoverageEnd;
    private String status;
    private String permissionIssue;
    private Integer armedScheduleCount;
    private List<String> armedScheduleIds;
    private Integer skippedScheduleCount;
    private List<AlarmStatusFailureDto> failures;
    private Instant updatedAt;
}
