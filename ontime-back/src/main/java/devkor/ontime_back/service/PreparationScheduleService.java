package devkor.ontime_back.service;

import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.repository.PreparationScheduleRepository;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PreparationScheduleService {
    private final ScheduleService scheduleService;
    private final PreparationScheduleRepository preparationScheduleRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;


    @Transactional
    public void makePreparationSchedules(Long userId, UUID scheduleId, List<PreparationDto> preparationDtoList) {
        handlePreparationSchedules(userId, scheduleId, preparationDtoList, false);
    }

    @Transactional
    public void updatePreparationSchedules(Long userId, UUID scheduleId, List<PreparationDto> preparationDtoList) {
        handlePreparationSchedules(userId, scheduleId, preparationDtoList, true);
    }

    @Transactional
    protected void handlePreparationSchedules(Long userId, UUID scheduleId, List<PreparationDto> preparationDtoList, boolean shouldDelete) {
        scheduleService.replaceScheduleCustomPreparations(userId, scheduleId, preparationDtoList, shouldDelete);
    }
}
