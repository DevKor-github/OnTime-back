package devkor.ontime_back.dto;

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
    private String deviceId;
    private OffsetDateTime reconciledAt;
    private LocalDateTime scheduleWindowStart;
    private LocalDateTime scheduleWindowEnd;
    private LocalDateTime alarmCoverageStart;
    private LocalDateTime alarmCoverageEnd;
    private String status;
    private String permissionIssue;
    private String nativeAlarmProvider;
    private String fallbackProvider;
    private Integer armedScheduleCount;
    private List<String> armedScheduleIds;
    private Integer skippedScheduleCount;
    private List<AlarmStatusFailureDto> failures;
}
