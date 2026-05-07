package devkor.ontime_back.dto;

import lombok.*;
import java.sql.Time;

@Getter
public class UserAdditionalInfoDto {
    private Integer spareTime;  // 여유시간
    private String note;     // 주의사항
    @Builder
    public UserAdditionalInfoDto(Integer spareTime, String note) {
        this.spareTime = spareTime;
        this.note = note;
    }
}
