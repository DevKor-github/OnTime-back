package devkor.ontime_back.dto;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
@Builder
public class UserInfoResponse {
    private Long userId;
    public String email;
    public String name;
    public Integer spareTime;
    public String note;
    public Float punctualityScore;
    public Role role;
    public SocialType socialType;
}
