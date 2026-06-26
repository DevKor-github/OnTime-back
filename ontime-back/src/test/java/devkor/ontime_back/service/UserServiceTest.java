package devkor.ontime_back.service;

import devkor.ontime_back.dto.UpdateSpareTimeDto;
import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.dto.UserAdditionalInfoDto;
import devkor.ontime_back.dto.UserOnboardingDto;
import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PreparationUserRepository preparationUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        preparationUserRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("성실도 점수 조회에 성공한다")
    @Test
    void getPunctualityScore(){
        // given(유저 데이터 하드저장 및 성실도 점수 조회를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(0.1f)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId();

        // when
        Float punctualityScore = userService.getPunctualityScore(targetId);

        // then
        assertThat(punctualityScore).isEqualTo(0.1f);

     }

    @DisplayName("성실도 점수를 조회할 때 존재하지 않는 유저id를 인자로 넘기는 경우 예외가 발생한다")
    @Test
    void getPunctualityScoreWithWrongUserId(){
        // given(유저 데이터 하드저장 및 성실도 점수 조회를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(0.1f)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId() +1;

         // when // then
         assertThatThrownBy(() -> userService.getPunctualityScore(targetId))
                 .isInstanceOf(IllegalArgumentException.class)
                 .hasMessage("존재하지 않는 유저 id입니다.");

    }


    @DisplayName("성실도 점수 초기화에 성공한다")
    @Test
    void resetPunctualityScore(){
        // given(유저 데이터 하드저장 및 성실도 점수 초기화를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(0.1f)
                .scheduleCountAfterReset(2)
                .latenessCountAfterReset(1)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId();

        // when
        Float punctualityScore = userService.resetPunctualityScore(targetId);

        // then
        User foundUser = userRepository.findById(targetId).orElseThrow(() -> new IllegalArgumentException());
        assertThat(punctualityScore).isEqualTo(-1f);
        assertThat(foundUser.getScheduleCountAfterReset()).isEqualTo(0);
        assertThat(foundUser.getLatenessCountAfterReset()).isEqualTo(0);
    }

    @DisplayName("성실도 점수를 초기화할 때 존재하지 않는 유저id를 인자로 넘기는 경우 예외가 발생한다")
    @Test
    void resetPunctualityScoreWithWrongUserId(){
        // given(유저 데이터 하드저장 및 성실도 점수 조회를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(0.1f)
                .scheduleCountAfterReset(1)
                .latenessCountAfterReset(1)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId() +1;

        // when // then
        assertThatThrownBy(() -> userService.resetPunctualityScore(targetId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");

    }


    @DisplayName("1. 성실도 점수 초기화(회원가입 포함) 직후 " +
            "2. 약속에 지각 하지 않았을 때 " +
            "성실도 점수 업데이트에 성공한다." +
            "(준비 종료 이후 /schedul/finish 엔드포인트에 의해 호출되는 메서드)")
    @Test
    void updatePunctualityFirstWithoutLateness(){
        // given(유저 데이터 하드저장 및 성실도 점수 업데이트를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId();

        // when
        User updatedUser = userService.updatePunctualityScore(targetId, DoneStatus.NORMAL);

        // then
        assertThat(updatedUser.getId()).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser)
                .extracting("punctualityScore", "scheduleCountAfterReset", "latenessCountAfterReset")
                .contains(calculatePunctualityScore(1, 0), 1, 0);
    }

    @DisplayName("1. 성실도 점수 초기화(회원가입 포함) 직후 " +
            "2. 약속에 지각 했을 때 " +
            "성실도 점수 업데이트에 성공한다." +
            "(준비 종료 이후 /schedul/finish 엔드포인트에 의해 호출되는 메서드)")
    @Test
    void updatePunctualityFirstWithLateness(){
        // given(유저 데이터 하드저장 및 성실도 점수 업데이트를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId();

        // when
        User updatedUser = userService.updatePunctualityScore(targetId, DoneStatus.LATE);

        // then
        assertThat(updatedUser.getId()).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser)
                .extracting("punctualityScore", "scheduleCountAfterReset", "latenessCountAfterReset")
                .contains(calculatePunctualityScore(1, 1), 1, 1);
    }

    @DisplayName("1. 기존 성실도 점수가 있을 때(초기화 값이 아닐 때) " +
            "2. 약속에 지각 하지 않았을 때 " +
            "성실도 점수 업데이트에 성공한다." +
            "(준비 종료 이후 /schedul/finish 엔드포인트에 의해 호출되는 메서드)")
    @Test
    void updatePunctualityNotFirstWithoutLateness(){
        // given(유저 데이터 하드저장 및 성실도 점수 업데이트를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(calculatePunctualityScore(3, 1))
                .scheduleCountAfterReset(3)
                .latenessCountAfterReset(1)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId();

        // when
        User updatedUser = userService.updatePunctualityScore(targetId, DoneStatus.NORMAL);

        // then
        assertThat(updatedUser.getId()).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser)
                .extracting("punctualityScore", "scheduleCountAfterReset", "latenessCountAfterReset")
                .contains(calculatePunctualityScore(4, 1), 4, 1);
    }

    @DisplayName("1. 기존 성실도 점수가 있을 때(초기화 값이 아닐 때) " +
            "2. 약속에 지각 했을 때 " +
            "성실도 점수 업데이트에 성공한다." +
            "(준비 종료 이후 /schedul/finish 엔드포인트에 의해 호출되는 메서드)")
    @Test
    void updatePunctualityNotFirstWithLateness(){
        // given(유저 데이터 하드저장 및 성실도 점수 업데이트를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(calculatePunctualityScore(3, 1))
                .scheduleCountAfterReset(3)
                .latenessCountAfterReset(1)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId();

        // when
        User updatedUser = userService.updatePunctualityScore(targetId, DoneStatus.LATE);

        // then
        assertThat(updatedUser.getId()).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser)
                .extracting("punctualityScore", "scheduleCountAfterReset", "latenessCountAfterReset")
                .contains(calculatePunctualityScore(4, 2), 4, 2);
    }

    @DisplayName("ABNORMAL 상태는 성실도 점수 계산에 포함하지 않는다.")
    @Test
    void updatePunctualityWithAbnormalDoesNotCount(){
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(calculatePunctualityScore(3, 1))
                .scheduleCountAfterReset(3)
                .latenessCountAfterReset(1)
                .build();
        userRepository.save(addedUser);

        User updatedUser = userService.updatePunctualityScore(addedUser.getId(), DoneStatus.ABNORMAL);

        assertThat(updatedUser.getPunctualityScore()).isCloseTo(calculatePunctualityScore(3, 1), offset(0.0001f));
        assertThat(updatedUser)
                .extracting("scheduleCountAfterReset", "latenessCountAfterReset")
                .contains(3, 1);
    }

    @DisplayName("성실도 점수 업데이트할 때 존재하지 않는 유저id를 인자로 넘기는 경우 예외가 발생한다.")
    @Test
    void updatePunctualityWithWrongUserId(){
        // given(유저 데이터 하드저장 및 성실도 점수 업데이트를 위한 targetId 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .punctualityScore(calculatePunctualityScore(3, 1))
                .scheduleCountAfterReset(3)
                .latenessCountAfterReset(1)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId() + 1;

        // when // then
        assertThatThrownBy(() -> userService.updatePunctualityScore(targetId, DoneStatus.LATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }


    @DisplayName("여유시간 업데이트에 성공한다")
    @Test
    void updateSpareTime(){
        // given(유저 데이터 하드저장 및 여유시간 업데이트를 위한 targetId 설정 및 updateSpareTimeDto 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .spareTime(10)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId();

        UpdateSpareTimeDto updateSpareTimeDto = UpdateSpareTimeDto.builder().newSpareTime(20).build();

        // when
        User updatedUser = userService.updateSpareTime(targetId, updateSpareTimeDto);

        // then
        assertThat(updatedUser.getId()).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getSpareTime()).isEqualTo(20);
    }

    @DisplayName("여유시간 업데이트할 때 존재하지 않는 유저id를 인자로 넘기는 경우 예외가 발생한다.")
    @Test
    void updateSpareTimeWithWrongUserId(){
        // given(유저 데이터 하드저장 및 여유시간 업데이트를 위한 targetId 설정 및 updateSpareTimeDto 설정)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .spareTime(10)
                .build();
        userRepository.save(addedUser);
        Long targetId = addedUser.getId() +1;

        UpdateSpareTimeDto updateSpareTimeDto = UpdateSpareTimeDto.builder().newSpareTime(20).build();

        // when // then
        assertThatThrownBy(() -> userService.updateSpareTime(targetId, updateSpareTimeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

    @DisplayName("온보딩은 유저 추가정보, 권한, 최초 준비과정을 함께 저장한다")
    @Test
    void onboardingStoresAdditionalInfoAuthorizesUserAndCreatesPreparations() throws Exception {
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .role(Role.GUEST)
                .build();
        userRepository.save(addedUser);
        UUID wakeUp = UUID.randomUUID();
        UUID shower = UUID.randomUUID();
        UserOnboardingDto onboardingDto = UserOnboardingDto.builder()
                .spareTime(15)
                .note("지하철 확인")
                .preparationList(List.of(
                        PreparationDto.builder()
                                .preparationId(wakeUp)
                                .preparationName("기상")
                                .preparationTime(5)
                                .nextPreparationId(shower)
                                .build(),
                        PreparationDto.builder()
                                .preparationId(shower)
                                .preparationName("샤워")
                                .preparationTime(10)
                                .build()
                ))
                .build();

        userService.onboarding(addedUser.getId(), onboardingDto);

        User onboardedUser = userRepository.findById(addedUser.getId()).orElseThrow();
        assertThat(onboardedUser.getSpareTime()).isEqualTo(15);
        assertThat(onboardedUser.getNote()).isEqualTo("지하철 확인");
        assertThat(onboardedUser.getRole()).isEqualTo(Role.USER);
        assertThat(preparationUserRepository.findAll())
                .extracting("preparationName")
                .containsExactlyInAnyOrder("기상", "샤워");
    }

    @DisplayName("온보딩은 DTO 허용 범위인 50자 준비과정 이름을 저장한다")
    @Test
    void onboardingStoresPreparationNameUpToDtoLimit() throws Exception {
        User addedUser = User.builder()
                .email("long-name@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .role(Role.GUEST)
                .build();
        userRepository.save(addedUser);
        String fiftyCharacterName = "a".repeat(50);

        UserOnboardingDto onboardingDto = UserOnboardingDto.builder()
                .spareTime(15)
                .note("note")
                .preparationList(List.of(PreparationDto.builder()
                        .preparationId(UUID.randomUUID())
                        .preparationName(fiftyCharacterName)
                        .preparationTime(5)
                        .build()))
                .build();

        userService.onboarding(addedUser.getId(), onboardingDto);

        assertThat(preparationUserRepository.findAll())
                .extracting("preparationName")
                .contains(fiftyCharacterName);
    }

    @DisplayName("존재하지 않는 유저는 온보딩할 수 없다")
    @Test
    void onboardingRejectsMissingUser() {
        UserOnboardingDto onboardingDto = UserOnboardingDto.builder()
                .spareTime(15)
                .note("note")
                .preparationList(List.of(PreparationDto.builder()
                        .preparationId(UUID.randomUUID())
                        .preparationName("기상")
                        .preparationTime(5)
                        .build()))
                .build();

        assertThatThrownBy(() -> userService.onboarding(404L, onboardingDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

    @DisplayName("유저 정보 조회는 저장된 사용자 프로필을 반환한다")
    @Test
    void getUserInfoReturnsPersistedUserProfile() {
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .spareTime(10)
                .note("note")
                .build();
        userRepository.save(addedUser);

        User userInfo = userService.getUserInfo(addedUser.getId());

        assertThat(userInfo.getId()).isEqualTo(addedUser.getId());
        assertThat(userInfo)
                .extracting("email", "name", "spareTime", "note")
                .contains("user@example.com", "junbeom", 10, "note");
    }

    @DisplayName("존재하지 않는 유저 정보 조회는 예외를 반환한다")
    @Test
    void getUserInfoRejectsMissingUser() {
        assertThatThrownBy(() -> userService.getUserInfo(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }


    float calculatePunctualityScore(int totalSchedules, int lateSchedules){
        return (1 - ((float) lateSchedules / totalSchedules)) * 100;
    }

}
