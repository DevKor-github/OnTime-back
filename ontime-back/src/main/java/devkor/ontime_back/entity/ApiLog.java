package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ApiLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ApiLogId;

    private String requestUrl;

    private String requestMethod;

    private String userId;

    private String clientIp;

    private int responseStatus;

    private long takenTime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}
