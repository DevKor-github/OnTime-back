package devkor.ontime_back.service;

import devkor.ontime_back.dto.PreparationDto;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class PreparationScheduleServiceTest {

    @Autowired
    private PreparationUserRepository preparationUserRepository;

    @Autowired
    private PreparationScheduleRepository preparationScheduleRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PreparationScheduleService preparationScheduleService;

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

    @Test
    @DisplayName("새로운 데이터로 준비과정 설정을 성공한다.")
    void makePreparationSchedules_withoutDeletingExisting() {
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

        UUID preparationSchedule1Id = UUID.randomUUID();
        UUID preparationSchedule2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationSchedule1Id, "세면", 10, preparationSchedule2Id),
                new PreparationDto(preparationSchedule2Id, "옷입기", 15, null)
        );

        // when
        preparationScheduleService.makePreparationSchedules(newUser.getId(), addedSchedule1.getScheduleId(), preparationDtoList);

        // then
        List<PreparationSchedule> savedPreparations = preparationScheduleRepository.findByScheduleWithNextPreparation(addedSchedule1);
        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationSchedule::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");
    }

    @Test
    @DisplayName("기존 준비 과정을 삭제한 후 새로운 준비과정 설정을 성공한다.")
    void updatePreparationSchedules_withDeletingExisting() {
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

        PreparationSchedule preparationSchedule3 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "화장", 10, null));
        PreparationSchedule preparationSchedule2= preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "아침식사", 10, preparationSchedule3));
        PreparationSchedule preparationSchedule1= preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "알림확인", 10, preparationSchedule2));

        UUID preparationSchedule1Id = UUID.randomUUID();
        UUID preparationSchedule2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationSchedule1Id, "세면", 10, preparationSchedule2Id),
                new PreparationDto(preparationSchedule2Id, "옷입기", 15, null)
        );

        // when
        preparationScheduleService.updatePreparationSchedules(newUser.getId(), addedSchedule1.getScheduleId(), preparationDtoList);

        // then
        List<PreparationSchedule> savedPreparations = preparationScheduleRepository.findByScheduleWithNextPreparation(addedSchedule1);
        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationSchedule::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");
    }

    @Test
    @DisplayName("기존 데이터를 삭제하지 않고 준비과정 설정을 성공한다.")
    void handlePreparationSchedules_withoutDeletingExisting() {
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

        UUID preparationSchedule1Id = UUID.randomUUID();
        UUID preparationSchedule2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationSchedule1Id, "세면", 10, preparationSchedule2Id),
                new PreparationDto(preparationSchedule2Id, "옷입기", 15, null)
        );

        // when
        preparationScheduleService.handlePreparationSchedules(newUser.getId(), addedSchedule1.getScheduleId(), preparationDtoList, false);

        // then
        List<PreparationSchedule> savedPreparations = preparationScheduleRepository.findByScheduleWithNextPreparation(addedSchedule1);
        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationSchedule::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");
    }

    @Test
    @DisplayName("기존 데이터를 삭제한 후 준비과정 설정을 성공한다.")
    void handlePreparationSchedules_withDeletingExisting() {
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

        PreparationSchedule preparationSchedule3 = preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "화장", 10, null));
        PreparationSchedule preparationSchedule2= preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "아침식사", 10, preparationSchedule3));
        PreparationSchedule preparationSchedule1= preparationScheduleRepository.save(new PreparationSchedule(
                UUID.randomUUID(), addedSchedule1, "알림확인", 10, preparationSchedule2));

        UUID newPreparationSchedule1Id = UUID.randomUUID();
        UUID newPreparationSchedule2Id = UUID.randomUUID();

        List<PreparationDto> newPreparationDtoList = List.of(
                new PreparationDto(newPreparationSchedule1Id, "세면", 10, newPreparationSchedule2Id),
                new PreparationDto(newPreparationSchedule2Id, "옷입기", 15, null)
        );

        // when
        preparationScheduleService.handlePreparationSchedules(newUser.getId(), addedSchedule1.getScheduleId(), newPreparationDtoList, true);

        // then
        List<PreparationSchedule> savedPreparations = preparationScheduleRepository.findByScheduleWithNextPreparation(addedSchedule1);

        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationSchedule::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");

        assertThat(savedPreparations).extracting(PreparationSchedule::getPreparationName)
                .doesNotContain("알림확인", "아침식사", "화장");
    }

    @Test
    @DisplayName("존재하지 않는 일정 ID로 요청 시 준비과정 설정을 실패한다.")
    void handlePreparationSchedules_withInvalidScheduleId() {
        // given
        Long userId = 1L;
        UUID invalidScheduleId = UUID.randomUUID();
        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(UUID.randomUUID(), "세면", 10, null)
        );

        // when & then
        assertThatThrownBy(() -> preparationScheduleService.handlePreparationSchedules(userId, invalidScheduleId, preparationDtoList, false))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.SCHEDULE_NOT_FOUND.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 사용자의 준비과정 설정시 실패한다.")
    void handlePreparationSchedules_withUnauthorizedUser() {
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
                .password(passwordEncoder.encode("password1234"))
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

        UUID preparationSchedule1Id = UUID.randomUUID();
        UUID preparationSchedule2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationSchedule1Id, "세면", 10, preparationSchedule2Id),
                new PreparationDto(preparationSchedule2Id, "옷입기", 15, null)
        );

        // when & then
        assertThatThrownBy(() -> preparationScheduleService.handlePreparationSchedules(newUser2.getId(), addedSchedule1.getScheduleId(), preparationDtoList, false))
                .isInstanceOf(GeneralException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED_ACCESS.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS);
    }


}

