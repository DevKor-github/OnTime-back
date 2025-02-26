package devkor.ontime_back.service;

import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.entity.*;
import devkor.ontime_back.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PreparationUserServiceTest {

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
    private PreparationUserService preparationUserService;

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
    @DisplayName("회원가입 시 기본 준비과정 설정 성공한다.")
    void setFirstPreparationUser_success() {
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

        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        // when
        preparationUserService.setFirstPreparationUser(newUser.getId(), preparationDtoList);

        // then
        List<PreparationUser> savedPreparations = preparationUserRepository.findByUserIdWithNextPreparation(newUser.getId());
        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationUser::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 회원가입 시 기본 준비과정 설정시 실패한다.")
    void setFirstPreparationUser_userNotFound() {
        // given
        Long userId = 1L;

        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        // when & then
        assertThatThrownBy(() -> preparationUserService.setFirstPreparationUser(userId, preparationDtoList))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("사용자 ID " + userId + "에 해당하는 사용자를 찾을 수 없습니다.");

    }

    @Test
    @DisplayName("준비과정 수정 성공한다.")
    void updatePreparationUsers_success() {
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

        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        // when
        preparationUserService.updatePreparationUsers(newUser.getId(), preparationDtoList);

        // then
        List<PreparationUser> savedPreparations = preparationUserRepository.findByUserIdWithNextPreparation(newUser.getId());
        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationUser::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 준비과정 수정시 실패한다.")
    void updatePreparationUsers_userNotFound() {
        // given
        Long userId = 1L;

        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        // when & then
        assertThatThrownBy(() -> preparationUserService.updatePreparationUsers(userId, preparationDtoList))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("사용자 ID " + userId + "에 해당하는 사용자를 찾을 수 없습니다.");

    }

    @Test
    @DisplayName("준비과정 조회를 성공한다.")
    void showAllPreparationUsers_success() {
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

        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();
        UUID preparationUser3Id = UUID.randomUUID();

        PreparationUser preparationUser3 = preparationUserRepository.save(new PreparationUser(
                preparationUser3Id, newUser, "화장", 10, null));
        PreparationUser preparationUser2= preparationUserRepository.save(new PreparationUser(
                preparationUser2Id, newUser, "아침식사", 10, preparationUser3));
        PreparationUser preparationUser1= preparationUserRepository.save(new PreparationUser(
                preparationUser1Id, newUser, "세면", 10, preparationUser2));

        // when
        List<PreparationDto> result = preparationUserService.showAllPreparationUsers(newUser.getId());

        // then
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(preparationUser1Id, result.get(0).getPreparationId());
        assertEquals("세면", result.get(0).getPreparationName());
        assertEquals(10, result.get(0).getPreparationTime());
        assertEquals(preparationUser2Id, result.get(0).getNextPreparationId());

        assertEquals(preparationUser2Id, result.get(1).getPreparationId());
        assertEquals("아침식사", result.get(1).getPreparationName());
        assertEquals(10, result.get(1).getPreparationTime());
        assertEquals(preparationUser3Id, result.get(1).getNextPreparationId());

        assertEquals(preparationUser3Id, result.get(2).getPreparationId());
        assertEquals("화장", result.get(2).getPreparationName());
        assertEquals(10, result.get(2).getPreparationTime());
        assertNull(result.get(2).getNextPreparationId());
    }

    @Test
    @DisplayName("준비과정이 없는 사용자가 준비과정 조회시 실패한다.")
    void showAllPreparationUsers_notFound() {
        // given
        Long userId = 1L;

        // when & then
        assertThatThrownBy(() -> preparationUserService.showAllPreparationUsers(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("사용자 ID " + userId + "에 대한 시작 준비 단계를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("기존 데이터를 삭제하지 않고 준비과정 설정을 성공한다.")
    void handlePreparationUsers_withoutDeletingExisting() {
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

        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();


        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        // when
        preparationUserService.handlePreparationUsers(newUser, preparationDtoList, false);

        // then
        List<PreparationUser> savedPreparations = preparationUserRepository.findByUserIdWithNextPreparation(newUser.getId());
        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationUser::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");
    }

    @Test
    @DisplayName("기존 데이터를 삭제하고 준비과정 설정을 성공한다.")
    void handlePreparationUsers_withDeletingExisting() {
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

        UUID preparationUser1Id = UUID.randomUUID();
        UUID preparationUser2Id = UUID.randomUUID();

        PreparationUser preparationUser3 = preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "화장", 10, null));
        PreparationUser preparationUser2= preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "아침식사", 10, preparationUser3));
        PreparationUser preparationUser1= preparationUserRepository.save(new PreparationUser(
                UUID.randomUUID(), newUser, "알림확인", 10, preparationUser2));

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(preparationUser1Id, "세면", 10, preparationUser2Id),
                new PreparationDto(preparationUser2Id, "옷입기", 15, null)
        );

        // when
        preparationUserService.handlePreparationUsers(newUser, preparationDtoList, true);

        // then
        List<PreparationUser> savedPreparations = preparationUserRepository.findByUserIdWithNextPreparation(newUser.getId());
        assertThat(savedPreparations).hasSize(2);
        assertThat(savedPreparations).extracting(PreparationUser::getPreparationName)
                .containsExactlyInAnyOrder("세면", "옷입기");
    }

}
