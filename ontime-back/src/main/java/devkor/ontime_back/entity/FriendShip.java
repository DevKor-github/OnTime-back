package devkor.ontime_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FriendShip {

    @Id
    private UUID friendShipId;

    @Column(nullable = false)
    private Long requesterId;

    @Column
    private Long receiverId;

    private String acceptStatus; // "PENDING", "ACCEPTED", "REJECTED"

    public void updateReceiverId(Long recieverId) {
        this.receiverId = recieverId;
    }

    public void updateAcceptStatus(String acceptStatus) {
        this.acceptStatus = acceptStatus;
    }
}
