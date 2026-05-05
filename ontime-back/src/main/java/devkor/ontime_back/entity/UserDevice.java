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
                @UniqueConstraint(name = "uk_user_device_user_device", columnNames = {"user_id", "device_id"})
        },
        indexes = {
                @Index(name = "idx_user_device_user_active", columnList = "user_id, active")
        }
)
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userDeviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(length = 128)
    private String appVersion;

    @Column(length = 128)
    private String osVersion;

    @Column(nullable = false)
    private Boolean supportsNativeAlarm;

    @Column(nullable = false, length = 40)
    private String nativeAlarmProvider;

    @Column(nullable = false, length = 40)
    private String fallbackProvider;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String firebaseToken;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String sessionAccessToken;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String sessionRefreshToken;

    public static UserDevice create(User user, String deviceId) {
        return UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .active(false)
                .build();
    }

    public void activate(String platform,
                         String appVersion,
                         String osVersion,
                         Boolean supportsNativeAlarm,
                         String nativeAlarmProvider,
                         String fallbackProvider,
                         Instant lastSeenAt) {
        this.platform = platform;
        this.appVersion = appVersion;
        this.osVersion = osVersion;
        this.supportsNativeAlarm = supportsNativeAlarm;
        this.nativeAlarmProvider = nativeAlarmProvider;
        this.fallbackProvider = fallbackProvider;
        this.active = true;
        this.lastSeenAt = lastSeenAt;
    }

    public void deactivate() {
        this.active = false;
        this.lastSeenAt = Instant.now();
    }

    public void bindSession(String accessToken, String refreshToken) {
        this.sessionAccessToken = accessToken;
        this.sessionRefreshToken = refreshToken;
    }

    public boolean belongsToAccessToken(String accessToken) {
        return sessionAccessToken != null && sessionAccessToken.equals(accessToken);
    }

    public void updateFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
        this.lastSeenAt = Instant.now();
    }
}
