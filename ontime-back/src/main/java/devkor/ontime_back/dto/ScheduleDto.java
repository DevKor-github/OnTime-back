package devkor.ontime_back.dto;

import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.Place;
import devkor.ontime_back.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.UUID;

@ToString
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

}
