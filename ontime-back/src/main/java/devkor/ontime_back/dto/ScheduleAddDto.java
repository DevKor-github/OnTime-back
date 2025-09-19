package devkor.ontime_back.dto;

import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.Place;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
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
public class ScheduleAddDto {
    private UUID scheduleId;
    private UUID placeId;
    private String placeName;
    private String scheduleName;
    private Integer moveTime; // 이동시간
    private LocalDateTime scheduleTime; // 약속시각
    private Boolean isChange; // 변경여부
    private Boolean isStarted; // 버튼누름여부
    private Integer scheduleSpareTime; // 스케줄 별 여유시간
    private String scheduleNote; // 스케줄 별 주의사항

    public Schedule toEntity(User user, Place place) {
        return Schedule.builder()
                .user(user)
                .scheduleId(this.scheduleId)
                .place(place)
                .scheduleName(this.scheduleName)
                .moveTime(this.moveTime)
                .scheduleTime(this.scheduleTime)
                .isChange(false)
                .isStarted(false)
                .scheduleSpareTime(this.scheduleSpareTime)
                .latenessTime(-1)
                .scheduleNote(this.scheduleNote)
                .doneStatus(DoneStatus.NOT_ENDED)
                .build();
    }
}
