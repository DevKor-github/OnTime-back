package devkor.ontime_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_analytics_preference_user", columnNames = "user_id")
        }
)
public class UserAnalyticsPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userAnalyticsPreferenceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Boolean userOverridden;

    public static UserAnalyticsPreference defaultFor(User user, boolean defaultEnabled) {
        return UserAnalyticsPreference.builder()
                .user(user)
                .enabled(defaultEnabled)
                .updatedAt(Instant.now())
                .userOverridden(false)
                .build();
    }

    @PrePersist
    private void initializeDefaults() {
        if (enabled == null) enabled = false;
        if (updatedAt == null) updatedAt = Instant.now();
        if (userOverridden == null) userOverridden = false;
    }

    public boolean alignToDefault(boolean defaultEnabled) {
        if (Boolean.TRUE.equals(userOverridden) || Objects.equals(enabled, defaultEnabled)) {
            return false;
        }
        this.enabled = defaultEnabled;
        this.updatedAt = Instant.now();
        return true;
    }

    public void update(boolean enabled) {
        this.enabled = enabled;
        this.userOverridden = true;
        this.updatedAt = Instant.now();
    }
}
