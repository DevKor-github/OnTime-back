package devkor.ontime_back.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendDto {
    private Long friendId;
    private String friendName;
    private String friendEmail;
    public FriendDto(Long friendId, String friendName, String friendEmail) {
        this.friendId = friendId;
        this.friendName = friendName;
        this.friendEmail = friendEmail;
    }
}
