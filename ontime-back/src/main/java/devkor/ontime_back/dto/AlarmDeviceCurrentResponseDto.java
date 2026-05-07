package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class AlarmDeviceCurrentResponseDto {
    private String deviceId;
    private Boolean active;
    private Instant lastSeenAt;
}
