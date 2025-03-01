package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
@AllArgsConstructor
public class UserSetting {
    @Id
    private UUID userSettingId; // 기본 키

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 ID (외래 키)

    @Column(nullable = false)
    private Boolean isNotificationsEnabled; // 앱 알림 허용 여부

    @Column(nullable = false)
    private Integer soundVolume; // 사운드 음량 (0~100)

    @Column(nullable = false)
    private Boolean isPlayOnSpeaker; // 내장 스피커에서 재생 여부

    @Column(nullable = false)
    private Boolean is24HourFormat; // 24시간제 사용 여부

    @PrePersist
    private void initializeDefaults() {
        if (isNotificationsEnabled == null) isNotificationsEnabled = true;
        if (soundVolume == null) soundVolume = 50;
        if (isPlayOnSpeaker == null) isPlayOnSpeaker = true;
        if (is24HourFormat == null) is24HourFormat = true;
    }

    public void updateUserSetting(Boolean isNotificationsEnabled, Integer soundVolume, Boolean isPlayOnSpeaker, Boolean is24HourFormat) {
        this.isNotificationsEnabled = isNotificationsEnabled;
        this.soundVolume = soundVolume;
        this.isPlayOnSpeaker = isPlayOnSpeaker;
        this.is24HourFormat = is24HourFormat;
    }
}
