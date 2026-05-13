package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StartScheduleResponseDto {
    private ScheduleDto schedule;
    private List<PreparationDto> preparations;
}
