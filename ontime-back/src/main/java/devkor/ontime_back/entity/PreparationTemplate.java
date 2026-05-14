package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        indexes = {
                @Index(name = "idx_preparation_template_user_deleted", columnList = "user_id, deleted_at"),
                @Index(name = "idx_preparation_template_created", columnList = "created_at")
        }
)
public class PreparationTemplate {
    @Id
    private UUID preparationTemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, length = 30)
    private String templateName;

    @Column(nullable = false, length = 30)
    private String normalizedTemplateName;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;

    @PrePersist
    private void initializeTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void update(String templateName, String normalizedTemplateName, Instant updatedAt) {
        this.templateName = templateName;
        this.normalizedTemplateName = normalizedTemplateName;
        this.updatedAt = updatedAt;
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.updatedAt = deletedAt;
    }
}
