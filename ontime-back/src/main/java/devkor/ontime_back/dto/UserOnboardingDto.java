package devkor.ontime_back.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class UserOnboardingDto {
    private Integer spareTime;
    private String note;
    private List<PreparationDto> preparationList;
}
