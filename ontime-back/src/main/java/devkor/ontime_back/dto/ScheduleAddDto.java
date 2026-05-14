package devkor.ontime_back.dto;

import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.Place;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAddDto {
    @NotNull(message = "scheduleId는 필수입니다.")
    private UUID scheduleId;
    @NotNull(message = "placeId는 필수입니다.")
    private UUID placeId;
    @NotBlank(message = "장소 이름은 필수입니다.")
    @Size(max = 100, message = "장소 이름은 100자 이하여야 합니다.")
    private String placeName;
    @NotBlank(message = "일정 이름은 필수입니다.")
    @Size(max = 30, message = "일정 이름은 30자 이하여야 합니다.")
    private String scheduleName;
    @NotNull(message = "이동 시간은 필수입니다.")
    @Min(value = 0, message = "이동 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "이동 시간은 1440 이하여야 합니다.")
    private Integer moveTime; // 이동시간
    @NotNull(message = "일정 시간은 필수입니다.")
    @FutureOrPresent(message = "일정 시간은 현재 또는 미래여야 합니다.")
    private LocalDateTime scheduleTime; // 약속시각
    private Boolean isChange; // 변경여부
    private Boolean isStarted; // 버튼누름여부
    private UUID preparationTemplateId;
    private List<@Valid OrderedPreparationDto> customPreparations;
    @Min(value = 0, message = "여유 시간은 0 이상이어야 합니다.")
    @Max(value = 1440, message = "여유 시간은 1440 이하여야 합니다.")
    private Integer scheduleSpareTime; // 스케줄 별 여유시간
    @Size(max = 1000, message = "일정 메모는 1000자 이하여야 합니다.")
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
                .startedAt(null)
                .finishedAt(null)
                .preparationMode(null)
                .preparationTemplate(null)
                .scheduleSpareTime(this.scheduleSpareTime)
                .latenessTime(-1)
                .scheduleNote(this.scheduleNote)
                .doneStatus(DoneStatus.NOT_ENDED)
                .build();
    }
}
