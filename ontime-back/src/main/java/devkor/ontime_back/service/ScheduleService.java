package devkor.ontime_back.service;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.entity.Place;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.*;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final UserService userService;

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PreparationScheduleRepository preparationScheduleRepository;
    private final PreparationUserRepository preparationUserRepository;

    // scheduleId, userId를 통한 권한 확인
    private Schedule getScheduleWithAuthorization(UUID scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findByIdWithUser(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 일정을 찾을 수 없습니다: " + scheduleId));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("사용자가 해당 일정에 대한 권한이 없습니다.");
        }

        return schedule;
    }

    // 특정 기간의 약속 조회
    public List<ScheduleDto> showSchedulesByPeriod(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Schedule> periodScheduleList;
        if (startDate == null && endDate != null) { // StartDate가 null인 경우, EndDate 이전의 일정 모두 반환
            periodScheduleList = scheduleRepository.findAllByUserIdAndScheduleTimeBefore(userId, endDate);
        } else if (endDate == null && startDate != null) { // EndDate가 null인 경우, StartDate 이후의 일정 모두 반환
            periodScheduleList = scheduleRepository.findAllByUserIdAndScheduleTimeAfter(userId, startDate);
        } else if (startDate != null && endDate != null) { // StartDate와 EndDate 모두 존재하는 경우, 해당 기간의 일정 반환
            periodScheduleList = scheduleRepository.findAllByUserIdAndScheduleTimeBetween(
                    userId, startDate, endDate);
        } else { // StartDate와 EndDate가 모두 null인 경우, 모든 일정 반환
            periodScheduleList = scheduleRepository.findAllByUserIdWithPlace(userId);
        }

        return periodScheduleList.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // schedule id에 따른 schedule 조회
    public ScheduleDto showScheduleByScheduleId(Long userId, UUID scheduleId) {
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        return mapToDto(schedule);
    }

    // schedule 삭제
    @Transactional
    public void deleteSchedule(UUID scheduleId, Long userId) {
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        preparationScheduleRepository.deleteBySchedule(schedule);
        scheduleRepository.deleteByScheduleId(scheduleId);
    }

    // schedule 수정
    @Transactional
    public void modifySchedule(Long userId, ScheduleModDto scheduleModDto) {
        Schedule schedule = getScheduleWithAuthorization(scheduleModDto.getScheduleId(), userId);

        Place place = placeRepository.findByPlaceName(scheduleModDto.getPlaceName())
                .orElseGet(() -> placeRepository.save(new Place(scheduleModDto.getPlaceId(), scheduleModDto.getPlaceName())));

        schedule.updateSchedule(
                place,
                scheduleModDto.getScheduleName(),
                scheduleModDto.getMoveTime(),
                scheduleModDto.getScheduleTime(),
                scheduleModDto.getScheduleSpareTime(),
                scheduleModDto.getLatenessTime(),
                scheduleModDto.getScheduleNote());
    }

    // schedule 추가
    @Transactional
    public void addSchedule(ScheduleAddDto scheduleAddDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 사용자를 찾을 수 없습니다: " + userId));
        Place place = placeRepository.findByPlaceName(scheduleAddDto.getPlaceName())
                .orElseGet(() -> placeRepository.save(new Place(scheduleAddDto.getPlaceId(), scheduleAddDto.getPlaceName())));

        Schedule schedule = Schedule.builder()
                .scheduleId(scheduleAddDto.getScheduleId())
                .user(user)
                .place(place)
                .scheduleName(scheduleAddDto.getScheduleName())
                .moveTime(scheduleAddDto.getMoveTime())
                .scheduleTime(scheduleAddDto.getScheduleTime())
                .scheduleSpareTime(scheduleAddDto.getScheduleSpareTime())
                .scheduleNote(scheduleAddDto.getScheduleNote())
                .isChange(false)
                .isStarted(false)
                .latenessTime(-1)
                .build();

        scheduleRepository.save(schedule);
    }

    // schedule 시작
    @Transactional
    public void checkIsStarted(UUID scheduleId, Long userId) {
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        schedule.startSchedule();
    }

    // 지각 히스토리 반환
    public List<LatenessHistoryResponse> getLatenessHistory(Long userId) {
        return scheduleRepository.findLatenessHistoryByUserId(userId).stream()
                .map(schedule -> new LatenessHistoryResponse(
                        schedule.getScheduleId(),
                        schedule.getScheduleName(),
                        schedule.getScheduleTime(),
                        schedule.getLatenessTime()
                ))
                .toList();
    }

    // 지각 시간 업데이트
    @Transactional
    public void updateLatenessTime(FinishPreparationDto finishPreparationDto) {
        UUID scheduleId = finishPreparationDto.getScheduleId();
        Integer latenessTime = finishPreparationDto.getLatenessTime();

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ErrorCode.SCHEDULE_NOT_FOUND));

        schedule.updateLatenessTime(latenessTime);
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void finishSchedule(Long userId, FinishPreparationDto finishPreparationDto) {
        updateLatenessTime(finishPreparationDto);
        userService.updatePunctualityScore(userId, finishPreparationDto.getLatenessTime());
    }

    // schedule에 따른 preparation 조회
    public List<PreparationDto> getPreparations(Long userId, UUID scheduleId) {
        Schedule schedule = getScheduleWithAuthorization(scheduleId, userId);

        if (Boolean.TRUE.equals(schedule.getIsChange())) {
            return preparationScheduleRepository.findByScheduleWithNextPreparation(schedule).stream()
                    .map(preparationSchedule -> new PreparationDto(
                            preparationSchedule.getPreparationScheduleId(),
                            preparationSchedule.getPreparationName(),
                            preparationSchedule.getPreparationTime(),
                            preparationSchedule.getNextPreparation() != null
                                    ? preparationSchedule.getNextPreparation().getPreparationScheduleId()
                                    : null
                    ))
                    .collect(Collectors.toList());
        } else {
            return preparationUserRepository.findByUserIdWithNextPreparation(schedule.getUser().getId()).stream()
                    .map(preparationUser -> new PreparationDto(
                            preparationUser.getPreparationId(),
                            preparationUser.getPreparationName(),
                            preparationUser.getPreparationTime(),
                            preparationUser.getNextPreparation() != null
                                    ? preparationUser.getNextPreparation().getPreparationId()
                                    : null
                    ))
                    .collect(Collectors.toList());
        }
    }

    private ScheduleDto mapToDto(Schedule schedule) {
        return new ScheduleDto(
                schedule.getScheduleId(),
                (schedule.getPlace() != null) ? new PlaceDto(schedule.getPlace().getPlaceId(), schedule.getPlace().getPlaceName()) : null,
                schedule.getScheduleName(),
                schedule.getMoveTime(),
                schedule.getScheduleTime(),
                schedule.getScheduleSpareTime(),
                schedule.getScheduleNote(),
                schedule.getLatenessTime()
        );
    }

}
