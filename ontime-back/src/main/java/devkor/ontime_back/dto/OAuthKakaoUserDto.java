package devkor.ontime_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OAuthKakaoUserDto {
    @NotBlank(message = "카카오 사용자 ID는 필수입니다.")
    @Size(max = 128, message = "카카오 사용자 ID는 128자 이하여야 합니다.")
    private String id;
    @Valid
    @NotNull(message = "프로필은 필수입니다.")
    private Profile profile;
    @Data
    public static class Profile {
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
        private String nickname;
        @Size(max = 1000, message = "썸네일 URL은 1000자 이하여야 합니다.")
        private String thumbnailImageUrl;
        @JsonProperty("profile_image_url")
        @Size(max = 1000, message = "프로필 이미지 URL은 1000자 이하여야 합니다.")
        private String profileImageUrl;
        private boolean isDefaultImage;
        private boolean isDefaultNickname;
    }
}
