package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class AnalyticsPreferenceResponseDto {
    private Boolean enabled;
    private Instant updatedAt;
}
