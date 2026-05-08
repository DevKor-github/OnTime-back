package devkor.ontime_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangePasswordDto {
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "현재 비밀번호는 8자 이상 64자 이하여야 합니다.")
    private String currentPassword;
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "새 비밀번호는 8자 이상 64자 이하여야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$", message = "새 비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.")
    private String newPassword;
    @Builder
    public ChangePasswordDto(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
