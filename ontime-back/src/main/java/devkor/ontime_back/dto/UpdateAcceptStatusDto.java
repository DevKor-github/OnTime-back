package devkor.ontime_back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateAcceptStatusDto {
    @NotBlank(message = "수락 상태는 필수입니다.")
    @Pattern(regexp = "ACCEPTED|REJECTED", message = "수락 상태는 ACCEPTED 또는 REJECTED만 가능합니다.")
    private String acceptStatus;
}
