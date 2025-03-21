
package devkor.ontime_back.service;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.entity.*;
import devkor.ontime_back.repository.*;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;


@SpringBootTest
class ScheduleServiceTest {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PreparationUserRepository preparationUserRepository;

    @Autowired
    private PreparationScheduleRepository preparationScheduleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        preparationUserRepository.deleteAll();
        preparationScheduleRepository.deleteAll();
        scheduleRepository.deleteAllInBatch();
        placeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("스케줄 id로 존재하는 스케줄을 조회 성공한다.")
    @Test
    void showScheduleByScheduleId_success() {

        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        Place place2 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5602"))
                .placeName("중식당")
                .build();

        Place place3 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5603"))
                .placeName("성수")
                .build();

        placeRepository.save(place1);
        placeRepository.save(place2);
        placeRepository.save(place3);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        Schedule addedSchedule2 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170002"))
                .scheduleName("가족행사")
                .scheduleTime(LocalDateTime.of(2025, 3, 15, 9, 0))
                .moveTime(40)
                .latenessTime(-1)
                .place(place2)
                .user(newUser)
                .build();

        Schedule addedSchedule3 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170003"))
                .scheduleName("친구약속")
                .scheduleTime(LocalDateTime.of(2025, 4, 9, 5, 0))
                .moveTime(35)
                .latenessTime(-1)
                .place(place3)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);
        scheduleRepository.save(addedSchedule2);
        scheduleRepository.save(addedSchedule3);

        // when
        ScheduleDto result = scheduleService.showScheduleByScheduleId(newUser.getId(), addedSchedule1.getScheduleId());

        // then
        assertNotNull(result);
        assertEquals("공부하기", result.getScheduleName());

    }

    @DisplayName("다른 사용자의 스케줄을 조회시 실패한다.")
    @Test
    void showScheduleByScheduleId_failByWrongUser() {

        // given
        User newUser1 = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser1);

        User newUser2 = User.builder()
                .email("user1@example.com")
                .password(passwordEncoder.encode("password1235"))
                .name("suhjin")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser2);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser1)
                .build();
        scheduleRepository.save(addedSchedule1);


        assertThatThrownBy(() -> scheduleService.showScheduleByScheduleId(newUser2.getId(), addedSchedule1.getScheduleId()))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_ACCESS.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);

    }

    @DisplayName("스케줄 id로 존재하지 않는 스케줄을 조회 실패한다.")
    @Test
    void showScheduleByScheduleId_failByNonExistentSchedule() {

        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();
        scheduleRepository.save(addedSchedule1);

        UUID randomScheduleId = UUID.fromString("023e4567-e89b-12d3-a456-426614170000");

        // when & then
        assertThatThrownBy(() -> scheduleService.showScheduleByScheduleId(newUser.getId(), randomScheduleId))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.SCHEDULE_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("전체 기간 지정하여 특정 기간의 약속 조회 성공한다.")
    void showSchedulesByPeriod_fullPeriod() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        Place place2 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5602"))
                .placeName("중식당")
                .build();

        Place place3 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5603"))
                .placeName("성수")
                .build();

        placeRepository.save(place1);
        placeRepository.save(place2);
        placeRepository.save(place3);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        Schedule addedSchedule2 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170002"))
                .scheduleName("가족행사")
                .scheduleTime(LocalDateTime.of(2025, 3, 15, 9, 0))
                .moveTime(40)
                .latenessTime(-1)
                .place(place2)
                .user(newUser)
                .build();

        Schedule addedSchedule3 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170003"))
                .scheduleName("친구약속")
                .scheduleTime(LocalDateTime.of(2025, 4, 9, 5, 0))
                .moveTime(35)
                .latenessTime(-1)
                .place(place3)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);
        scheduleRepository.save(addedSchedule2);
        scheduleRepository.save(addedSchedule3);

        // when
        List<ScheduleDto> result = scheduleService.showSchedulesByPeriod(
                newUser.getId(),
                LocalDateTime.of(2025, 3, 1, 0, 0),
                LocalDateTime.of(2025, 4, 10, 23, 59)
        );

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("StartDate 이후 일정 조회하여 특정 기간의 약속 조회 성공한다.")
    void showSchedulesByPeriod_startDateOnly() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        Place place2 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5602"))
                .placeName("중식당")
                .build();

        Place place3 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5603"))
                .placeName("성수")
                .build();

        placeRepository.save(place1);
        placeRepository.save(place2);
        placeRepository.save(place3);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        Schedule addedSchedule2 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170002"))
                .scheduleName("가족행사")
                .scheduleTime(LocalDateTime.of(2025, 3, 15, 9, 0))
                .moveTime(40)
                .latenessTime(-1)
                .place(place2)
                .user(newUser)
                .build();

        Schedule addedSchedule3 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170003"))
                .scheduleName("친구약속")
                .scheduleTime(LocalDateTime.of(2025, 4, 9, 5, 0))
                .moveTime(35)
                .latenessTime(-1)
                .place(place3)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);
        scheduleRepository.save(addedSchedule2);
        scheduleRepository.save(addedSchedule3);

        // when
        List<ScheduleDto> result = scheduleService.showSchedulesByPeriod(
                newUser.getId(),
                LocalDateTime.of(2025, 3, 16, 0, 0),
                null
        );

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("endDate 이전 일정 조회하여 특정 기간의 약속 조회 성공한다.")
    void showSchedulesByPeriod_endDateOnly() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        Place place2 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5602"))
                .placeName("중식당")
                .build();

        Place place3 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5603"))
                .placeName("성수")
                .build();

        placeRepository.save(place1);
        placeRepository.save(place2);
        placeRepository.save(place3);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        Schedule addedSchedule2 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170002"))
                .scheduleName("가족행사")
                .scheduleTime(LocalDateTime.of(2025, 3, 15, 9, 0))
                .moveTime(40)
                .latenessTime(-1)
                .place(place2)
                .user(newUser)
                .build();

        Schedule addedSchedule3 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170003"))
                .scheduleName("친구약속")
                .scheduleTime(LocalDateTime.of(2025, 4, 9, 5, 0))
                .moveTime(35)
                .latenessTime(-1)
                .place(place3)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);
        scheduleRepository.save(addedSchedule2);
        scheduleRepository.save(addedSchedule3);

        // when
        List<ScheduleDto> result = scheduleService.showSchedulesByPeriod(
                newUser.getId(),
                null,
                LocalDateTime.of(2025, 3, 1, 0, 0)
        );

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("StartDate와 EndDate가 모두 null인 일정 조회하여 특정 기간의 약속 조회 성공한다.")
    void showSchedulesByPeriod_allNull() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        Place place2 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5602"))
                .placeName("중식당")
                .build();

        Place place3 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5603"))
                .placeName("성수")
                .build();

        placeRepository.save(place1);
        placeRepository.save(place2);
        placeRepository.save(place3);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        Schedule addedSchedule2 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170002"))
                .scheduleName("가족행사")
                .scheduleTime(LocalDateTime.of(2025, 3, 15, 9, 0))
                .moveTime(40)
                .latenessTime(-1)
                .place(place2)
                .user(newUser)
                .build();

        Schedule addedSchedule3 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170003"))
                .scheduleName("친구약속")
                .scheduleTime(LocalDateTime.of(2025, 4, 9, 5, 0))
                .moveTime(35)
                .latenessTime(-1)
                .place(place3)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);
        scheduleRepository.save(addedSchedule2);
        scheduleRepository.save(addedSchedule3);

        // when
        List<ScheduleDto> result = scheduleService.showSchedulesByPeriod(
                newUser.getId(),
                null,
                null
        );

        // then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("주어진 기간 내 일정 없는 경우 특정 기간의 약속 조회 시 빈 결과를 낸다.")
    void showSchedulesByPeriod_noSchedulesInRange() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        Place place2 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5602"))
                .placeName("중식당")
                .build();

        Place place3 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5603"))
                .placeName("성수")
                .build();

        placeRepository.save(place1);
        placeRepository.save(place2);
        placeRepository.save(place3);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        Schedule addedSchedule2 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170002"))
                .scheduleName("가족행사")
                .scheduleTime(LocalDateTime.of(2025, 3, 15, 9, 0))
                .moveTime(40)
                .latenessTime(-1)
                .place(place2)
                .user(newUser)
                .build();

        Schedule addedSchedule3 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170003"))
                .scheduleName("친구약속")
                .scheduleTime(LocalDateTime.of(2025, 4, 9, 5, 0))
                .moveTime(35)
                .latenessTime(-1)
                .place(place3)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);
        scheduleRepository.save(addedSchedule2);
        scheduleRepository.save(addedSchedule3);

        // when
        List<ScheduleDto> result = scheduleService.showSchedulesByPeriod(
                newUser.getId(),
                LocalDateTime.of(2025, 4, 15, 0, 0),
                LocalDateTime.of(2025, 5, 10, 0, 0)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하는 약속을 삭제 성공한다.")
    void deleteSchedule_success() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);


        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);

        // when
        scheduleService.deleteSchedule(addedSchedule1.getScheduleId(), newUser.getId());

        // then
        Optional<Schedule> deletedSchedule = scheduleRepository.findById(addedSchedule1.getScheduleId());
        assertThat(deletedSchedule).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자가 약속 삭제 시도 시 실패한다.")
    void deleteSchedule_failByWrongUser() {
        // given
        User newUser1 = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser1);

        User newUser2 = User.builder()
                .email("user1@example.com")
                .password(passwordEncoder.encode("password1235"))
                .name("suhjin")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser2);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser1)
                .build();

        scheduleRepository.save(addedSchedule1);

        // when & then
        assertThatThrownBy(() -> scheduleService.showScheduleByScheduleId(newUser2.getId(), addedSchedule1.getScheduleId()))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_ACCESS.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);

        assertThat(scheduleRepository.findById(addedSchedule1.getScheduleId())).isPresent();

    }

    @Test
    @DisplayName("존재하지 않는 약속을 삭제 시도 시 실패한다.")
    void deleteSchedule_failByNonExistentSchedule() {
        // given
        User newUser1 = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser1);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser1)
                .build();

        scheduleRepository.save(addedSchedule1);

        UUID randomScheduleId = UUID.fromString("023e4567-e89b-12d3-a456-426614170000");

        // when & then
        assertThatThrownBy(() -> scheduleService.deleteSchedule(randomScheduleId, newUser1.getId()))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.SCHEDULE_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);

    }

    @Test
    @DisplayName("존재하는 약속을 수정 성공한다.")
    void modifySchedule_success() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);


        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);

        ScheduleModDto scheduleModDto = ScheduleModDto.builder()
                .scheduleName("친구랑 약속")
                .scheduleTime(LocalDateTime.of(2025, 2, 24, 14, 0))
                .moveTime(20)
                .scheduleNote("늦으면 안됨")
                .placeId(place1.getPlaceId())
                .placeName(place1.getPlaceName())
                .scheduleSpareTime(5)
                .latenessTime(10)
                .build();

        // when
        scheduleService.modifySchedule(newUser.getId(), addedSchedule1.getScheduleId(), scheduleModDto);

        // then
        Schedule updatedSchedule = scheduleRepository.findById(addedSchedule1.getScheduleId()).orElseThrow();
        assertThat(updatedSchedule.getScheduleName()).isEqualTo("친구랑 약속");
        assertThat(updatedSchedule.getScheduleTime()).isEqualTo(LocalDateTime.of(2025, 2, 24, 14, 0));
        assertThat(updatedSchedule.getScheduleNote()).isEqualTo("늦으면 안됨");
        assertThat(updatedSchedule.getMoveTime()).isEqualTo(20);
        assertThat(updatedSchedule.getScheduleSpareTime()).isEqualTo(5);
        assertThat(updatedSchedule.getLatenessTime()).isEqualTo(10);

    }

    @Test
    @DisplayName("새로운 장소를 추가하면서 약속 수정 성공한다.")
    void modifySchedule_withNewPlace() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);

        UUID scheduleId = UUID.fromString("023e4567-e89b-12d3-a456-426614170001");
        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(scheduleId)
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);

        long beforePlaceCount = placeRepository.count();

        ScheduleModDto scheduleModDto = ScheduleModDto.builder()
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .scheduleNote("늦으면 안됨")
                .placeId(UUID.fromString("80d460da-6a82-4c57-a285-567cdeda5711"))
                .placeName("애기능생활관")
                .scheduleSpareTime(5)
                .latenessTime(10)
                .build();

        // when
        scheduleService.modifySchedule(newUser.getId(), scheduleId, scheduleModDto);

        // then
        // DB에 저장 확인
        long afterPlaceCount = placeRepository.count();
        assertThat(afterPlaceCount).isEqualTo(beforePlaceCount + 1);

        Optional<Place> newPlace = placeRepository.findById(UUID.fromString("80d460da-6a82-4c57-a285-567cdeda5711"));
        assertThat(newPlace).isPresent();
        assertThat(newPlace.get().getPlaceName()).isEqualTo("애기능생활관");
    }

    @Test
    @DisplayName("다른 사용자가 약속 수정 시도 시 실패한다.")
    void modifySchedule_failByWrongUser() {
        // given
        User newUser1 = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser1);

        User newUser2 = User.builder()
                .email("user1@example.com")
                .password(passwordEncoder.encode("password1235"))
                .name("suhjin")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser2);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);


        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser1)
                .build();

        scheduleRepository.save(addedSchedule1);

        ScheduleModDto scheduleModDto = ScheduleModDto.builder()
                .scheduleName("친구랑 약속")
                .scheduleTime(LocalDateTime.of(2025, 2, 24, 14, 0)) // 시간 변경
                .moveTime(20)
                .scheduleNote("늦으면 안됨")
                .placeId(place1.getPlaceId())
                .placeName(place1.getPlaceName())
                .scheduleSpareTime(5)
                .latenessTime(10)
                .build();

        // when & then
        assertThatThrownBy(() -> scheduleService.modifySchedule(newUser2.getId(), addedSchedule1.getScheduleId(), scheduleModDto))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED_ACCESS.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);

        Schedule unchangedSchedule = scheduleRepository.findById(addedSchedule1.getScheduleId()).orElseThrow();
        assertThat(unchangedSchedule.getScheduleName()).isEqualTo("공부하기");

    }

    @Test
    @DisplayName("존재하지 않는 약속 수정 시도 시 실패한다.")
    void modifySchedule_failByNonExistentSchedule() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);


        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .place(place1)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);

        UUID randomScheduleId = UUID.fromString("023e4567-e89b-12d3-a456-426614170000");

        ScheduleModDto scheduleModDto = ScheduleModDto.builder()
                .scheduleName("친구랑 약속")
                .scheduleTime(LocalDateTime.of(2025, 2, 24, 14, 0)) // 시간 변경
                .moveTime(20)
                .scheduleNote("늦으면 안됨")
                .placeId(place1.getPlaceId())
                .placeName(place1.getPlaceName())
                .scheduleSpareTime(5)
                .latenessTime(10)
                .build();

        // when & then
        assertThatThrownBy(() -> scheduleService.modifySchedule(newUser.getId(), randomScheduleId, scheduleModDto))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.SCHEDULE_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);

    }

    @Test
    @DisplayName("기존 장소 사용하여 약속 추가 성공한다.")
    void addSchedule_withExistingPlace() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);

        ScheduleAddDto scheduleAddDto = ScheduleAddDto.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170000"))
                .scheduleName("수학시험")
                .scheduleTime(LocalDateTime.of(2025, 2, 10, 14, 0))
                .moveTime(30)
                .scheduleNote("늦으면 안됨")
                .placeId(place1.getPlaceId())
                .placeName(place1.getPlaceName())
                .scheduleSpareTime(5)
                .isChange(false)
                .isStarted(false)
                .build();

        // when
        scheduleService.addSchedule(scheduleAddDto, newUser.getId());

        // then (약속이 정상적으로 저장되었는지 확인)
        Optional<Schedule> savedSchedule = scheduleRepository.findById(scheduleAddDto.getScheduleId());
        assertThat(savedSchedule).isPresent();
        assertThat(savedSchedule.get().getScheduleName()).isEqualTo("수학시험");
        assertThat(savedSchedule.get().getScheduleTime()).isEqualTo(LocalDateTime.of(2025, 2, 10, 14, 0));
        assertThat(savedSchedule.get().getScheduleNote()).isEqualTo("늦으면 안됨");
        assertThat(savedSchedule.get().getMoveTime()).isEqualTo(30);
        assertThat(savedSchedule.get().getScheduleSpareTime()).isEqualTo(5);

    }

    @Test
    @DisplayName("새로운 장소를 추가하면서 약속 추가 성공한다.")
    void addSchedule_withNewPlace() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        long beforePlaceCount = placeRepository.count();

        ScheduleAddDto scheduleAddDto = ScheduleAddDto.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170000"))
                .scheduleName("수학시험")
                .scheduleTime(LocalDateTime.of(2025, 2, 10, 14, 0))
                .moveTime(30)
                .scheduleNote("늦으면 안됨")
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5511"))
                .placeName("고려대학교")
                .scheduleSpareTime(5)
                .isChange(false)
                .isStarted(false)
                .build();

        // when
        scheduleService.addSchedule(scheduleAddDto, newUser.getId());

        // then
        long afterPlaceCount = placeRepository.count();
        assertThat(afterPlaceCount).isEqualTo(beforePlaceCount + 1);

        Optional<Place> savedPlace = placeRepository.findById(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5511"));
        assertThat(savedPlace).isPresent();
        assertThat(savedPlace.get().getPlaceName()).isEqualTo("고려대학교");

        Optional<Schedule> savedSchedule = scheduleRepository.findByIdWithPlace(scheduleAddDto.getScheduleId());
        assertThat(savedSchedule).isPresent();
        assertThat(savedSchedule.get().getPlace().getPlaceName()).isEqualTo("고려대학교");
    }

    @Test
    @DisplayName("다른 사용자가 약속 추가 시 실패한다.")
    void addSchedule_failByNonExistentUser() {
        // given
        User newUser1 = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser1);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();
        placeRepository.save(place1);

        ScheduleAddDto scheduleAddDto = ScheduleAddDto.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170000"))
                .scheduleName("수학시험")
                .scheduleTime(LocalDateTime.of(2025, 2, 10, 14, 0))
                .moveTime(30)
                .scheduleNote("늦으면 안됨")
                .placeId(place1.getPlaceId())
                .placeName(place1.getPlaceName())
                .scheduleSpareTime(5)
                .isChange(false)
                .isStarted(false)
                .build();

        Long nonExistentUserId = 9999L;

        // when & then
        assertThatThrownBy(() -> scheduleService.addSchedule(scheduleAddDto, nonExistentUserId))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @DisplayName("지각 히스토리 조회에 성공한다")
    @Test
    void getLatenessHistory(){
        // given(유저, 스케줄1,2,3 데이터 하드저장)
        //      (비즈니스 로직에 따르면 스케줄 추가되면 지각시간은 -1로 자동으로 초기화됨.)
        //      (                      이후 스케줄이 종료되면 지각시간이 0 or 양수로 업데이트됨.)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .scheduleName("을사년 새해")
                .scheduleTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .latenessTime(3)
                .user(addedUser)
                .build();

        Schedule addedSchedule2 = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe6"))
                .scheduleName("생일파티")
                .scheduleTime(LocalDateTime.of(2025, 1, 12, 21, 0))
                .latenessTime(1)
                .user(addedUser)
                .build();

        Schedule addedSchedule3 = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe7"))
                .scheduleName("Ontime 출시일")
                .scheduleTime(LocalDateTime.of(2025, 2, 14, 00, 0))
                .latenessTime(-1)
                .user(addedUser)
                .build();

        scheduleRepository.save(addedSchedule1);
        scheduleRepository.save(addedSchedule2);
        scheduleRepository.save(addedSchedule3);

        // when
        List<LatenessHistoryResponse> latenessHistory = scheduleService.getLatenessHistory(addedUser.getId());

        // then
        assertThat(latenessHistory).hasSize(2)
                .extracting("scheduleId", "scheduleName", "scheduleTime", "latenessTime")
                .containsExactlyInAnyOrder(
                        tuple(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"), "을사년 새해", LocalDateTime.of(2025, 1, 1, 0, 0), 3),
                        tuple(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe6"), "생일파티", LocalDateTime.of(2025, 1, 12, 21, 0), 1)
                );
    }

    @DisplayName("지각 시간 업데이트에 성공한다.")
    @Test
    void updateLatenessTime(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        Schedule addedSchedule = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .scheduleName("을사년 새해")
                .scheduleTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .latenessTime(-1)
                .user(addedUser)
                .build();
        scheduleRepository.save(addedSchedule);

        FinishPreparationDto finishPreparationDto = FinishPreparationDto.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .latenessTime(1)
                .build();

        // when
        scheduleService.updateLatenessTime(finishPreparationDto);

        // then
        assertThat(scheduleRepository.findById(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .get().getLatenessTime()).isEqualTo(1);
    }

    @DisplayName("지각 시간 업데이트할 때, 잘못된 schedulId를 DTO에 담아 요청하는 경우 예외가 발생한다.")
    @Test
    void updateLatenessTimeWithWrongScheduleId(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        Schedule addedSchedule = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .scheduleName("을사년 새해")
                .scheduleTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .latenessTime(-1)
                .user(addedUser)
                .build();
        scheduleRepository.save(addedSchedule);

        FinishPreparationDto finishPreparationDto = FinishPreparationDto.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe6"))
                .latenessTime(1)
                .build();

        // when // then
        assertThatThrownBy(() -> scheduleService.updateLatenessTime(finishPreparationDto))
                .isInstanceOf(GeneralException.class)
                .hasMessage("해당 약속이 존재하지 않습니다.");
    }

    @DisplayName("약속을 종료해 지각시간과 성실도점수 업데이트에 성공한다.")
    @Test
    void finishSchedule(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        Schedule addedSchedule = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .scheduleName("을사년 새해")
                .scheduleTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .latenessTime(-1)
                .user(addedUser)
                .build();
        scheduleRepository.save(addedSchedule);

        FinishPreparationDto finishPreparationDto = FinishPreparationDto.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .latenessTime(1)
                .build();

        // when
        scheduleService.finishSchedule(addedUser.getId(), finishPreparationDto);

        // then
        assertThat(scheduleRepository.findById(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .get().getLatenessTime()).isEqualTo(1);
        assertThat(userRepository.findById(addedUser.getId()).get().getPunctualityScore()).isEqualTo(0f);
    }

    @DisplayName("약속을 종료할 때, 잘못된 유저id를 인자로 넘기는 경우 예외가 발생한다.")
    @Test
    void finishScheduleWithWrongUserId(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        Schedule addedSchedule = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .scheduleName("을사년 새해")
                .scheduleTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .latenessTime(-1)
                .user(addedUser)
                .build();
        scheduleRepository.save(addedSchedule);

        FinishPreparationDto finishPreparationDto = FinishPreparationDto.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .latenessTime(1)
                .build();

        // when // then
        assertThatThrownBy(() -> scheduleService.finishSchedule(addedUser.getId() + 1, finishPreparationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

    @DisplayName("약속을 종료할 때, 잘못된 scheduleId를 인자로 넘기는 경우 예외가 발생한다.")
    @Test
    void finishScheduleWithWrongScheduleId(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);

        Schedule addedSchedule = Schedule.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .scheduleName("을사년 새해")
                .scheduleTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .latenessTime(-1)
                .user(addedUser)
                .build();
        scheduleRepository.save(addedSchedule);

        FinishPreparationDto finishPreparationDto = FinishPreparationDto.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe6"))
                .latenessTime(1)
                .build();

        // when // then
        assertThatThrownBy(() -> scheduleService.finishSchedule(addedUser.getId(), finishPreparationDto))
                .isInstanceOf(GeneralException.class)
                .hasMessage("해당 약속이 존재하지 않습니다.");
    }

    @Test
    @DisplayName("isChange = true 상태에서 preparationScheduleRepository를 통한 조회 성공한다.")
    void getPreparations_success_whenIsChangeTrue() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .isStarted(true)
                .isChange(true)
                .place(place1)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);

        PreparationUser preparationUser2 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "옷입기", 30, null));
        PreparationUser preparationUser1 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "세면", 10, preparationUser2));


        PreparationSchedule preparationSchedule3 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "화장", 10, null));
        PreparationSchedule preparationSchedule2 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "아침식사", 20, preparationSchedule3));
        PreparationSchedule preparationSchedule1 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "세면", 15, preparationSchedule2));

        // when
        List<PreparationDto> result = scheduleService.getPreparations(newUser.getId(), addedSchedule1.getScheduleId());

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting("preparationName").containsExactlyInAnyOrder("세면", "아침식사", "화장");
    }

    @Test
    @DisplayName("isChange = false 상태에서 preparationUserRepository를 통한 조회 성공한다.")
    void getPreparations_success_whenIsChangeFalse() {
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .isStarted(false)
                .isChange(false)
                .place(place1)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);

        PreparationUser preparationUser2 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "옷입기", 30, null));
        PreparationUser preparationUser1 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "세면", 10, preparationUser2));


        PreparationSchedule preparationSchedule3 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "화장", 10, null));
        PreparationSchedule preparationSchedule2 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "아침식사", 20, preparationSchedule3));
        PreparationSchedule preparationSchedule1 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "세면", 15, preparationSchedule2));

        // when
        List<PreparationDto> result = scheduleService.getPreparations(newUser.getId(), addedSchedule1.getScheduleId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("preparationName").containsExactlyInAnyOrder("세면", "옷입기");
    }

    @Test
    @DisplayName("존재하지 않는 약속의 준비과정 조회시 실패한다.")
    void getPreparations_failByNonExistentSchedule(){
        // given
        User newUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .isStarted(false)
                .isChange(false)
                .place(place1)
                .user(newUser)
                .build();

        scheduleRepository.save(addedSchedule1);

        PreparationUser preparationUser2 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "옷입기", 30, null));
        PreparationUser preparationUser1 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "세면", 10, preparationUser2));


        PreparationSchedule preparationSchedule3 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "화장", 10, null));
        PreparationSchedule preparationSchedule2 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "아침식사", 20, preparationSchedule3));
        PreparationSchedule preparationSchedule1 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "세면", 15, preparationSchedule2));

        // when & then
        UUID randomScheduleId = UUID.randomUUID();
        assertThatThrownBy(() -> scheduleService.getPreparations(newUser.getId(), randomScheduleId))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.SCHEDULE_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자가 약속의 준비과정 조회시 실패한다.")
    void getPreparations_failByWrongUser(){
        // given
        User newUser1 = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("jinsuh")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser1);

        User newUser2 = User.builder()
                .email("user1@example.com")
                .password(passwordEncoder.encode("password1235"))
                .name("suhjin")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(newUser2);

        Place place1 = Place.builder()
                .placeId(UUID.fromString("70d460da-6a82-4c57-a285-567cdeda5601"))
                .placeName("과학도서관")
                .build();

        placeRepository.save(place1);

        Schedule addedSchedule1 = Schedule.builder()
                .scheduleId(UUID.fromString("023e4567-e89b-12d3-a456-426614170001"))
                .scheduleName("공부하기")
                .scheduleTime(LocalDateTime.of(2025, 2, 23, 7, 0))
                .moveTime(10)
                .latenessTime(-1)
                .isStarted(false)
                .isChange(false)
                .place(place1)
                .user(newUser1)
                .build();

        scheduleRepository.save(addedSchedule1);

        PreparationUser preparationUser2 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser1, "옷입기", 30, null));
        PreparationUser preparationUser1 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser1, "세면", 10, preparationUser2));


        PreparationSchedule preparationSchedule3 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "화장", 10, null));
        PreparationSchedule preparationSchedule2 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "아침식사", 20, preparationSchedule3));
        PreparationSchedule preparationSchedule1 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "세면", 15, preparationSchedule2));

        // when & then
        assertThatThrownBy(() -> scheduleService.getPreparations(newUser2.getId(), addedSchedule1.getScheduleId()))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_ACCESS.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);
    }
}
