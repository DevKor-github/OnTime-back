package devkor.ontime_back.entity;

import devkor.ontime_back.dto.ScheduleModDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@NoArgsConstructor // 기본 생성자 생성
@AllArgsConstructor // 모든 필드를 포함하는 생성자 생성
@Table(
        indexes = {
                @Index(name = "idx_schedule_user_id", columnList = "user_id"),
                @Index(name = "idx_schedule_time", columnList = "schedule_time")
        }
)
public class Schedule {

    @Id
    private UUID scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Place place;

    @Column(nullable = false, length = 30)
    private String scheduleName;

    private Integer moveTime; // 이동시간

    private LocalDateTime scheduleTime; // 약속시각

    private Boolean isChange; // 변경여부

    private Boolean isStarted; // 버튼누름여부

    @Enumerated(EnumType.STRING)
    private DoneStatus doneStatus;

    private Integer scheduleSpareTime; // 스케줄 별 여유시간

    private Integer latenessTime; // 지각 시간 (NULL이면 약속 전, 0이면 약속 성공, N(양수)면 N분 지각)

    @Lob // 대용량 텍스트 필드
    @Column(columnDefinition = "TEXT") // 명시적으로 TEXT 타입으로 정의
    private String scheduleNote; // 스케줄 별 주의사항

    public void updateSchedule(Place place, ScheduleModDto scheduleModDto) {
        this.place = place;
        this.scheduleName = scheduleModDto.getScheduleName();
        this.moveTime = scheduleModDto.getMoveTime();
        this.scheduleTime = scheduleModDto.getScheduleTime();
        this.scheduleSpareTime = scheduleModDto.getScheduleSpareTime();
        this.latenessTime = scheduleModDto.getLatenessTime();
        this.scheduleNote = scheduleModDto.getScheduleNote();
    }

    public void startSchedule() {
        this.isStarted = true;
    }

    public void changePreparationSchedule() {this.isChange = true;}

    public void updateLatenessTime(Integer latenessTime) {
        this.latenessTime = latenessTime;

        if (latenessTime > 0) {
            this.doneStatus = DoneStatus.LATE;
        } else if (latenessTime == 0) {
            this.doneStatus = DoneStatus.NORMAL;
        } else if (latenessTime == -1) {
            this.doneStatus = DoneStatus.NOT_ENDED;
        }
        else {
            this.doneStatus = DoneStatus.ABNORMAL;
        }
    }
}


