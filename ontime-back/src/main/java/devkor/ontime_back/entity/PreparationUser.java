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
public class PreparationUser {
    @Id
    private UUID preparationUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, length = 30)
    private String preparationName;

    private Integer preparationTime;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_preparation_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private PreparationUser nextPreparation;

    public void updateNextPreparation(PreparationUser nextPreparation) {
        this.nextPreparation = nextPreparation;
    }

}
