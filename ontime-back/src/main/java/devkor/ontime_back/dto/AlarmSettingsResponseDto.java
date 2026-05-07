package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class AlarmSettingsResponseDto {
    private Boolean alarmsEnabled;
    private Integer defaultAlarmOffsetMinutes;
    private Instant updatedAt;
}
