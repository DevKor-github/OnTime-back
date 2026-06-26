package devkor.ontime_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_refresh_token_token", columnNames = "refresh_token")
        },
        indexes = {
                @Index(name = "idx_user_refresh_token_user", columnList = "user_id")
        }
)
public class UserRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRefreshTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "refresh_token", nullable = false, length = 768)
    private String refreshToken;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static UserRefreshToken create(User user, String refreshToken) {
        Instant now = Instant.now();
        return UserRefreshToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void rotate(String refreshToken) {
        this.refreshToken = refreshToken;
        this.updatedAt = Instant.now();
    }
}
