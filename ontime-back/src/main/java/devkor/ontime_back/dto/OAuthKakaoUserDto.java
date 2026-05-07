package devkor.ontime_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OAuthKakaoUserDto {
    private String id;
    private Profile profile;
    @Data
    public static class Profile {
        private String nickname;
        private String thumbnailImageUrl;
        @JsonProperty("profile_image_url")
        private String profileImageUrl;
        private boolean isDefaultImage;
        private boolean isDefaultNickname;
    }
}