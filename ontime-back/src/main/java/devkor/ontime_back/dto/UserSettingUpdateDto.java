package devkor.ontime_back.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSettingUpdateDto {
    private Boolean isNotificationsEnabled;
    @Min(value = 0, message = "소리 크기는 0 이상이어야 합니다.")
    @Max(value = 100, message = "소리 크기는 100 이하여야 합니다.")
    private Integer soundVolume;
    private Boolean isPlayOnSpeaker;
    private Boolean is24HourFormat;
}
