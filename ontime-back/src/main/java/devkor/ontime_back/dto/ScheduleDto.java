package devkor.ontime_back.dto;

import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.PreparationMode;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드를 포함하는 생성자 추가
public class ScheduleDto {
    private UUID scheduleId;
    private PlaceDto place;
    private String scheduleName;
    private Integer moveTime;
    private LocalDateTime scheduleTime;
    private Integer scheduleSpareTime;
    private String scheduleNote;
    private Integer latenessTime;
    private DoneStatus doneStatus;
    private Instant startedAt;
    private Instant finishedAt;
    private PreparationMode preparationMode;
    private UUID preparationTemplateId;
    private String preparationTemplateName;
    private Boolean preparationTemplateDeleted;
    private Boolean preparationFrozen;

    public ScheduleDto(UUID scheduleId, PlaceDto place, String scheduleName, Integer moveTime,
                       LocalDateTime scheduleTime, Integer scheduleSpareTime, String scheduleNote,
                       Integer latenessTime, DoneStatus doneStatus) {
        this(scheduleId, place, scheduleName, moveTime, scheduleTime, scheduleSpareTime,
                scheduleNote, latenessTime, doneStatus, null, null, null, null, null, null, null);
    }

    public ScheduleDto(UUID scheduleId, PlaceDto place, String scheduleName, Integer moveTime,
                       LocalDateTime scheduleTime, Integer scheduleSpareTime, String scheduleNote,
                       Integer latenessTime, DoneStatus doneStatus, Instant startedAt) {
        this(scheduleId, place, scheduleName, moveTime, scheduleTime, scheduleSpareTime,
                scheduleNote, latenessTime, doneStatus, startedAt, null, null, null, null, null, null);
    }
}
