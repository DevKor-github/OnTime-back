package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_alarm_setting_user", columnNames = "user_id")
        }
)
public class UserAlarmSetting {

    public static final int DEFAULT_ALARM_OFFSET_MINUTES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userAlarmSettingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private Boolean alarmsEnabled;

    @Column(nullable = false)
    private Integer defaultAlarmOffsetMinutes;

    @Column(nullable = false)
    private Instant updatedAt;

    public static UserAlarmSetting defaultFor(User user) {
        return UserAlarmSetting.builder()
                .user(user)
                .alarmsEnabled(true)
                .defaultAlarmOffsetMinutes(DEFAULT_ALARM_OFFSET_MINUTES)
                .updatedAt(Instant.now())
                .build();
    }

    @PrePersist
    private void initializeDefaults() {
        if (alarmsEnabled == null) alarmsEnabled = true;
        if (defaultAlarmOffsetMinutes == null) defaultAlarmOffsetMinutes = DEFAULT_ALARM_OFFSET_MINUTES;
        if (updatedAt == null) updatedAt = Instant.now();
    }

    public void update(Boolean alarmsEnabled, Integer defaultAlarmOffsetMinutes) {
        if (alarmsEnabled != null) {
            this.alarmsEnabled = alarmsEnabled;
        }
        if (defaultAlarmOffsetMinutes != null) {
            this.defaultAlarmOffsetMinutes = defaultAlarmOffsetMinutes;
        }
        this.updatedAt = Instant.now();
    }
}
