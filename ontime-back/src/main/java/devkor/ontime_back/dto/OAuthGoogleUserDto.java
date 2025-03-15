package devkor.ontime_back.dto;

import lombok.Getter;

@Getter
public class OAuthGoogleUserDto {

    private String id;           // 고유 사용자 ID
    private String name;          // 사용자 이름
    private String picture;       // 프로필 이미지 URL
    private String email;         // 이메일

    public OAuthGoogleUserDto(String id, String name, String picture, String email) {
        this.id = id;
        this.name = name;
        this.picture = picture;
        this.email = email;
    }
}
