package devkor.ontime_back.dto;

import lombok.Getter;

@Getter
public class UpdatePunctualityScoreDto {
    private Long userId;
    private Integer latenessTime;
}
