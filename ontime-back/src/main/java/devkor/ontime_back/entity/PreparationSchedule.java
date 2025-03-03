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
public class PreparationSchedule {
    @Id
    private UUID preparationScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Schedule schedule;

    @Column(nullable = false, length = 30)
    private String preparationName;

    private Integer preparationTime;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_preparation_id")
    private PreparationSchedule nextPreparation;

    public void updateNextPreparation(PreparationSchedule nextPreparation) {
        this.nextPreparation = nextPreparation;
    }
}
