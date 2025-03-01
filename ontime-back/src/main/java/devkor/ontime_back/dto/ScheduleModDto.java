package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@ToString
@Getter
@Builder
@AllArgsConstructor
public class ScheduleModDto {

    private UUID scheduleId;

    private UUID placeId;

    private String placeName;

    private String scheduleName;

    private Integer moveTime; // 이동시간

    private LocalDateTime scheduleTime; // 약속시각

    private Integer scheduleSpareTime; // 스케줄 별 여유시간

    private Integer latenessTime;

    private String scheduleNote; // 스케줄 별 주의사항

}
