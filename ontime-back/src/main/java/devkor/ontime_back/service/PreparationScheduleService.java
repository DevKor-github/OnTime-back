package devkor.ontime_back.service;

import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.entity.PreparationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.PreparationScheduleRepository;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 일정을 찾을 수 없습니다: " + scheduleId));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("사용자가 해당 일정에 대한 권한이 없습니다.");
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
