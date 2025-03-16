package devkor.ontime_back.dto;

import lombok.Getter;

@Getter
public class OAuthGoogleRequestDto {
    private String idToken;
    private String refreshToken;
}
