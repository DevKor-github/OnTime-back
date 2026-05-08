package devkor.ontime_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuthGoogleRequestDto {
    @NotBlank(message = "idToken은 필수입니다.")
    @Size(max = 8192, message = "idToken은 8192자 이하여야 합니다.")
    private String idToken;
    @Size(max = 8192, message = "refreshToken은 8192자 이하여야 합니다.")
    private String refreshToken;
}
