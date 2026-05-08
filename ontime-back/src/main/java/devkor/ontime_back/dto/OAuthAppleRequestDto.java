package devkor.ontime_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuthAppleRequestDto {
    @NotBlank(message = "idToken은 필수입니다.")
    @Size(max = 8192, message = "idToken은 8192자 이하여야 합니다.")
    private String idToken;
    @NotBlank(message = "authCode는 필수입니다.")
    @Size(max = 8192, message = "authCode는 8192자 이하여야 합니다.")
    private String authCode;
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String fullName;
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
    private String email;
}
