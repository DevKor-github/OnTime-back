package devkor.ontime_back.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GetFriendListResponse {

    private List<FriendDto> friendsList;

}
