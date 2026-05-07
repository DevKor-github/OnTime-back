package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccountDeletionFeedback {

    @Id
    private UUID feedbackId;

    private Long deletedUserId;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Column(length = 64)
    private String emailHash;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;
}
