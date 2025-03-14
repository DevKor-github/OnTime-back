package devkor.ontime_back.service;

import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.entity.PreparationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.PreparationScheduleRepository;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static devkor.ontime_back.response.ErrorCode.SCHEDULE_NOT_FOUND;
import static devkor.ontime_back.response.ErrorCode.UNAUTHORIZED_ACCESS;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PreparationScheduleService {
    private final PreparationScheduleRepository preparationScheduleRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final JwtTokenProvider jwtTokenProvider;


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
        Schedule schedule = scheduleRepository.findByIdWithUser(scheduleId)
                .orElseThrow(() -> new GeneralException(SCHEDULE_NOT_FOUND));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new GeneralException(UNAUTHORIZED_ACCESS);
        }

        if (shouldDelete) {
            preparationScheduleRepository.deleteBySchedule(schedule);
        }

        schedule.changePreparationSchedule();
        scheduleRepository.save(schedule);

        Map<UUID, PreparationSchedule> preparationMap = new HashMap<>();

        List<PreparationSchedule> preparationSchedules = preparationDtoList.stream()
                .map(dto -> {
                    PreparationSchedule preparation = new PreparationSchedule(
                            dto.getPreparationId(),
                            schedule,
                            dto.getPreparationName(),
                            dto.getPreparationTime(),
                            null);
                    preparationMap.put(dto.getPreparationId(), preparation);
                    return preparation;
                })
                .collect(Collectors.toList());

        preparationScheduleRepository.saveAll(preparationSchedules);

        preparationDtoList.stream()
                .filter(dto -> dto.getNextPreparationId() != null)
                .forEach(dto -> {
                    PreparationSchedule current = preparationMap.get(dto.getPreparationId());
                    PreparationSchedule nextPreparation = preparationMap.get(dto.getNextPreparationId());
                    if (nextPreparation != null) {
                        current.updateNextPreparation(nextPreparation);
                    }
                });

        preparationScheduleRepository.saveAll(preparationSchedules);
    }
}
