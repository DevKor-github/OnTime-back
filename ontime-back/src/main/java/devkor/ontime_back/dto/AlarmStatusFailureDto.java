package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStatusFailureDto {
    private String scheduleId;
    private String reason;
}
