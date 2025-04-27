package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class NotificationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime notificationTime;

    private Boolean isSent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Builder
    public NotificationSchedule(LocalDateTime notificationTime, Boolean isSent, Schedule schedule) {
        this.notificationTime = notificationTime;
        this.isSent = isSent;
        this.schedule = schedule;
    }

    public void changeStatusToSent() {
        if(Boolean.FALSE.equals(this.isSent)) {
            this.isSent = true;
        }
    }

    public void updateNotificationTime(LocalDateTime localDateTime) {
        this.notificationTime = localDateTime;
    }

    public void markAsUnsent() {
        this.isSent = false;
    }

    public void disconnectSchedule() {
        this.schedule = null;
    }
}
