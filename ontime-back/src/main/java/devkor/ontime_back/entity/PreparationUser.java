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

    @Column(nullable = false, length = 50)
    private String preparationName;

    private Integer preparationTime;

    @Column(name = "order_index")
    private Integer orderIndex;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_preparation_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private PreparationUser nextPreparation;

    public void updateNextPreparation(PreparationUser nextPreparation) {
        this.nextPreparation = nextPreparation;
    }

    public PreparationUser(UUID preparationUserId, User user, String preparationName, Integer preparationTime, PreparationUser nextPreparation) {
        this(preparationUserId, user, preparationName, preparationTime, null, nextPreparation);
    }

}
