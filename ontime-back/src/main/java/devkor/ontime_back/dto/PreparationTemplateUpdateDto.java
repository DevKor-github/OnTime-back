package devkor.ontime_back.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparationTemplateUpdateDto {
    @NotBlank(message = "템플릿 이름은 필수입니다.")
    @Size(max = 30, message = "템플릿 이름은 30자 이하여야 합니다.")
    private String templateName;

    @NotEmpty(message = "준비과정은 하나 이상 필요합니다.")
    private List<@Valid OrderedPreparationDto> preparations;
}
