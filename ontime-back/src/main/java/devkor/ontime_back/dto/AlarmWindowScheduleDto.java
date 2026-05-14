package devkor.ontime_back.dto;

import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.PreparationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AlarmWindowScheduleDto {
    private UUID scheduleId;
    private String scheduleName;
    private PlaceDto place;
    private LocalDateTime scheduleTime;
    private Integer moveTime;
    private Integer scheduleSpareTime;
    private DoneStatus doneStatus;
    private Instant startedAt;
    private Instant finishedAt;
    private PreparationMode preparationMode;
    private UUID preparationTemplateId;
    private String preparationTemplateName;
    private Boolean preparationTemplateDeleted;
    private Boolean preparationFrozen;
    private LocalDateTime preparationStartTime;
    private LocalDateTime defaultAlarmTime;
    private List<PreparationDto> preparations;
    private Object alarmSettings;
}
