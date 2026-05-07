package devkor.ontime_back.dto;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class SchedulePeriodDto {
    LocalDateTime startDate;
    LocalDateTime endDate;
}
