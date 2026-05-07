package devkor.ontime_back.dto;

import lombok.Getter;

@Getter
public class OAuthAppleUserDto {
    private String appleUserId;
    private String email;
    private String fullName;
    public OAuthAppleUserDto(String appleUserId, String email, String fullName) {
        this.appleUserId = appleUserId;
        this.email = email;
        this.fullName = fullName;
    }
}
