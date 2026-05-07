package devkor.ontime_back.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class GetFriendshipRequesterResponse {
    private Long requesterId;
    private String requesterName;
    private String requesterEmail;
    @Builder
    public GetFriendshipRequesterResponse(Long requesterId, String requesterName, String requesterEmail) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
    }
}
