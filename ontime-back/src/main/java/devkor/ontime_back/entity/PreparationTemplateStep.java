package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "idx_preparation_template_step_template_order", columnList = "preparation_template_id, order_index")
})
public class PreparationTemplateStep {
    @Id
    private UUID preparationTemplateStepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preparation_template_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PreparationTemplate preparationTemplate;

    @Column(nullable = false, length = 50)
    private String preparationName;

    @Column(nullable = false)
    private Integer preparationTime;

    @Column(nullable = false)
    private Integer orderIndex;
}
