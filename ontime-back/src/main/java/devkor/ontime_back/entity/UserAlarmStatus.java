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
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_alarm_status_device", columnNames = "user_device_id")
        }
)
public class UserAlarmStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userAlarmStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_device_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserDevice userDevice;

    @Column(nullable = false, length = 128)
    private String deviceId;

    @Column(nullable = false)
    private Instant reconciledAt;

    private LocalDateTime scheduleWindowStart;
    private LocalDateTime scheduleWindowEnd;
    private LocalDateTime alarmCoverageStart;
    private LocalDateTime alarmCoverageEnd;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(length = 60)
    private String permissionIssue;

    @Column(nullable = false, length = 40)
    private String nativeAlarmProvider;

    @Column(nullable = false, length = 40)
    private String fallbackProvider;

    @Column(nullable = false)
    private Integer armedScheduleCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String armedScheduleIds;

    @Column(nullable = false)
    private Integer skippedScheduleCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String failures;

    @Column(nullable = false)
    private Instant updatedAt;

    public static UserAlarmStatus create(User user, UserDevice userDevice) {
        return UserAlarmStatus.builder()
                .user(user)
                .userDevice(userDevice)
                .deviceId(userDevice.getDeviceId())
                .build();
    }

    public void replace(Instant reconciledAt,
                        LocalDateTime scheduleWindowStart,
                        LocalDateTime scheduleWindowEnd,
                        LocalDateTime alarmCoverageStart,
                        LocalDateTime alarmCoverageEnd,
                        String status,
                        String permissionIssue,
                        String nativeAlarmProvider,
                        String fallbackProvider,
                        Integer armedScheduleCount,
                        String armedScheduleIds,
                        Integer skippedScheduleCount,
                        String failures) {
        this.deviceId = userDevice.getDeviceId();
        this.reconciledAt = reconciledAt;
        this.scheduleWindowStart = scheduleWindowStart;
        this.scheduleWindowEnd = scheduleWindowEnd;
        this.alarmCoverageStart = alarmCoverageStart;
        this.alarmCoverageEnd = alarmCoverageEnd;
        this.status = status;
        this.permissionIssue = permissionIssue;
        this.nativeAlarmProvider = nativeAlarmProvider;
        this.fallbackProvider = fallbackProvider;
        this.armedScheduleCount = armedScheduleCount;
        this.armedScheduleIds = armedScheduleIds;
        this.skippedScheduleCount = skippedScheduleCount;
        this.failures = failures;
        this.updatedAt = Instant.now();
    }
}
