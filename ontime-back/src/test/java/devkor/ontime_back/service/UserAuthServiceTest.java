package devkor.ontime_back.service;

import com.google.firebase.auth.UserInfo;
import devkor.ontime_back.dto.ChangePasswordDto;
import devkor.ontime_back.dto.FeedbackAddDto;
import devkor.ontime_back.dto.UserAdditionalInfoDto;
import devkor.ontime_back.dto.UserInfoResponse;
import devkor.ontime_back.dto.UserSignUpDto;
import devkor.ontime_back.entity.AccountDeletionFeedback;
import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.Feedback;
import devkor.ontime_back.entity.FriendShip;
import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Place;
import devkor.ontime_back.entity.PreparationSchedule;
import devkor.ontime_back.entity.PreparationUser;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.entity.UserAlarmStatus;
import devkor.ontime_back.entity.UserDevice;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.repository.AccountDeletionFeedbackRepository;
import devkor.ontime_back.repository.FeedbackRepository;
import devkor.ontime_back.repository.FriendshipRepository;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import devkor.ontime_back.repository.PlaceRepository;
import devkor.ontime_back.repository.PreparationScheduleRepository;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserAlarmStatusRepository;
import devkor.ontime_back.repository.UserDeviceRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.repository.UserSettingRepository;
import devkor.ontime_back.response.GeneralException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class UserAuthServiceTest {

    @Autowired
    private UserAuthService userAuthService;
    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSettingRepository userSettingRepository;
    @Autowired
    private AccountDeletionFeedbackRepository accountDeletionFeedbackRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private NotificationScheduleRepository notificationScheduleRepository;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private PreparationScheduleRepository preparationScheduleRepository;
    @Autowired
    private PreparationUserRepository preparationUserRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private UserAlarmSettingRepository userAlarmSettingRepository;
    @Autowired
    private UserAlarmStatusRepository userAlarmStatusRepository;
    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    private ServletRequest httpServletRequest;
    @Autowired
    private HttpServletResponse httpServletResponse;

    @AfterEach
    void tearDown() {
        accountDeletionFeedbackRepository.deleteAllInBatch();
        userAlarmStatusRepository.deleteAllInBatch();
        userDeviceRepository.deleteAllInBatch();
        notificationScheduleRepository.deleteAllInBatch();
        preparationScheduleRepository.deleteAllInBatch();
        scheduleRepository.deleteAllInBatch();
        preparationUserRepository.deleteAllInBatch();
        feedbackRepository.deleteAllInBatch();
        friendshipRepository.deleteAllInBatch();
        userAlarmSettingRepository.deleteAllInBatch();
        userSettingRepository.deleteAllInBatch();
        placeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입에 성공해 유저정보가 성공적으로 저장된다.")
    @Test
    void signUp() throws Exception {
        // given
        UserSignUpDto userSignUpDto = getUserSignUpDto("user@example.com", "password1234", "junbeom");

        // when
        UserInfoResponse userSignupResponse = userAuthService.signUp((HttpServletRequest) httpServletRequest, httpServletResponse, userSignUpDto);

        // then
        assertThat(userSignupResponse.getUserId()).isNotNull();
        assertThat(userSignupResponse)
                .extracting("email", "name", "spareTime", "note", "punctualityScore", "role")
                .contains("user@example.com", "junbeom", null, null, -1f, Role.GUEST);

        Optional<User> addedUser = userRepository.findById(userSignupResponse.getUserId());
        assertThat(addedUser.isPresent()).isTrue();
        User user = addedUser.get();
        assertThat(user)
                .extracting("email", "imageUrl", "name", "spareTime", "note", "punctualityScore", "scheduleCountAfterReset", "latenessCountAfterReset","role")
                .contains("user@example.com", null, "junbeom", null, null, -1f, 0, 0, Role.GUEST);
        assertThat(passwordEncoder.matches("password1234", user.getPassword())).isTrue();
        assertThat(user.getRefreshToken()).isNotNull();
        assertThat(user.getUserSetting()).isNotNull();
    }

    @DisplayName("이미 존재하는 이메일로 회원가입을 시도하는 경우 예외가 발생한다.")
    @Test
    void signUpWithExistingEmail() throws Exception {
        // given
        UserSignUpDto userSignUpDto1 = getUserSignUpDto("user@example.com", "password1234", "junbeom");

        UserSignUpDto userSignUpDto2 = getUserSignUpDto("user@example.com", "password1234", "junbeom2");

        // when, then
        UserInfoResponse userSignupResponse = userAuthService.signUp((HttpServletRequest) httpServletRequest, httpServletResponse,userSignUpDto1);
        assertThat(userSignupResponse.getUserId()).isNotNull();

        assertThatThrownBy(() -> userAuthService.signUp((HttpServletRequest) httpServletRequest, httpServletResponse,userSignUpDto2))
                .isInstanceOf(GeneralException.class)
                .hasMessage("이미 존재하는 이메일입니다.");
    }

    @DisplayName("이미 존재하는 이름으로 회원가입을 시도하는 경우 예외가 발생한다.")
    @Test
    void signUpWithExistingName() throws Exception {
        // given
        UserSignUpDto userSignUpDto1 = getUserSignUpDto("user@example.com", "password1234", "junbeom");

        UserSignUpDto userSignUpDto2 = getUserSignUpDto("user2@example.com", "password1234", "junbeom");

        // when, then
        UserInfoResponse userSignupResponse = userAuthService.signUp((HttpServletRequest) httpServletRequest, httpServletResponse,userSignUpDto1);
        assertThat(userSignupResponse.getUserId()).isNotNull();

        assertThatThrownBy(() -> userAuthService.signUp((HttpServletRequest) httpServletRequest, httpServletResponse,userSignUpDto2))
                .isInstanceOf(GeneralException.class)
                .hasMessage("이미 존재하는 이름입니다.");
    }

    @DisplayName("(회원가입 직후) 유저의 추가정보 기입에 성공한다")
    @Test
    void addInfo() throws Exception {
        // given(유저데이터 하드저장 및 추가 기입 DTO 생성)
        User addedUser = User.builder()
                .email("user@example.com")
                .password("password1234")
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        UserAdditionalInfoDto userAdditionalInfoDto = UserAdditionalInfoDto.builder()
                .spareTime(10)
                .note("내 인생에 이제 지각은 없다!!!")
                .build();

        // when(추가정보 기입)
        User additionalInfoAddedUser = userAuthService.addInfo(addedUser.getId(), userAdditionalInfoDto);

        // then
        assertThat(additionalInfoAddedUser.getId()).isNotNull();
        assertThat(additionalInfoAddedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(additionalInfoAddedUser)
                .extracting("spareTime", "note")
                .contains(10, "내 인생에 이제 지각은 없다!!!");
    }

    @DisplayName("존재하지 않는 유저의 추가정보를 기입하는 경우 예외가 발생한다")
    @Test
    void addInfoWithWrongUser() throws Exception {
        // given(유저데이터 하드저장 및 추가 기입 DTO 생성)
        User addedUser = User.builder()
                .email("user@example.com")
                .password("password1234")
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        UserAdditionalInfoDto userAdditionalInfoDto = UserAdditionalInfoDto.builder()
                .spareTime(10)
                .note("내 인생에 이제 지각은 없다!!!")
                .build();

        // when // then
        assertThatThrownBy(() -> userAuthService.addInfo(2L, userAdditionalInfoDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

    @DisplayName("비밀번호 변경에 성공한다")
    @Test
    void changePassword(){
        // given(유저 데이터 하드저장 및 비밀번호 변경 DTO 생성)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password1234")
                .newPassword("password12345")
                .build();

        // when
        User passwordChangedUser = userAuthService.changePassword(addedUser.getId(), changePasswordDto);

        // then
        assertThat(passwordChangedUser.getId()).isNotNull();
        assertThat(passwordChangedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(passwordEncoder.matches("password12345", passwordChangedUser.getPassword())).isTrue();
     }

    @DisplayName("비밀번호 변경 시 존재하지 않는 유저id를 인자로 넘기는 경우 예외가 발생한다.")
    @Test
    void changePasswordWithWrongUserId(){
        // given(유저 데이터 하드저장 및 비밀번호 변경 DTO 생성)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password1234")
                .newPassword("password12345")
                .build();

        // when // then
        assertThatThrownBy(() -> userAuthService.changePassword(2L, changePasswordDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("존재하지 않는 유저 id입니다.");
    }

    @DisplayName("비밀번호 변경시 현재 비밀번호를 잘못 입력한 경우 예외가 발생한다.")
    @Test
    void changePasswordWithWrongCurrentPassword(){
        // given(유저 데이터 하드저장 및 비밀번호 변경 DTO 생성)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password123")
                .newPassword("password12345")
                .build();

        // when // then
        assertThatThrownBy(() -> userAuthService.changePassword(addedUser.getId(), changePasswordDto))
                .isInstanceOf(GeneralException.class)
                .hasMessage("기존 비밀번호가 틀렸습니다.");
    }

    @DisplayName("비밀번호 변경시 새 비밀번호를 현재 비밀번호와 같게 입력한 경우 예외가 발생한다.")
    @Test
    void changePasswordWithSamePassword(){
        // given(유저 데이터 하드저장 및 비밀번호 변경 DTO 생성)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("password1234")
                .newPassword("password1234")
                .build();

        // when // then
        assertThatThrownBy(() -> userAuthService.changePassword(addedUser.getId(), changePasswordDto))
                .isInstanceOf(GeneralException.class)
                .hasMessage("새 비밀번호와 기존 비밀번호가 일치합니다.");
    }

    @DisplayName("계정 삭제에 성공한다")
    @Test
    void deleteUser(){
        // given(유저 데이터 하드저장 및 검증을 위해 해당 유저 아이디 tagerUserId에 저장)
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        Long targetUserId = addedUser.getId();

        // when
        Long deletedUserId = userAuthService.deleteUser(addedUser.getId());

        // then
        assertThat(deletedUserId).isNotNull();
        assertThat(deletedUserId).isEqualTo(targetUserId);
        assertThat(userRepository.findById(targetUserId)).isEmpty();
    }

    @DisplayName("계정 삭제 시 선택 피드백을 익명화하여 저장한다")
    @Test
    void deleteUserWithFeedback(){
        // given
        User addedUser = User.builder()
                .email("USER@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .socialType(SocialType.GOOGLE)
                .build();
        userRepository.save(addedUser);

        UUID feedbackId = UUID.randomUUID();
        FeedbackAddDto feedbackAddDto = FeedbackAddDto.builder()
                .feedbackId(feedbackId)
                .message("탈퇴 이유입니다.")
                .build();

        Long targetUserId = addedUser.getId();

        // when
        Long deletedUserId = userAuthService.deleteUser(targetUserId, feedbackAddDto);

        // then
        assertThat(deletedUserId).isEqualTo(targetUserId);
        assertThat(userRepository.findById(targetUserId)).isEmpty();

        AccountDeletionFeedback feedback = accountDeletionFeedbackRepository.findById(feedbackId)
                .orElseThrow();
        assertThat(feedback.getDeletedUserId()).isEqualTo(targetUserId);
        assertThat(feedback.getSocialType()).isEqualTo(SocialType.GOOGLE);
        assertThat(feedback.getMessage()).isEqualTo("탈퇴 이유입니다.");
        assertThat(feedback.getEmailHash()).hasSize(64);
        assertThat(feedback.getEmailHash()).doesNotContain("USER@example.com");
        assertThat(feedback.getCreatedAt()).isNotNull();
    }

    @DisplayName("계정 삭제 시 피드백이 없어도 삭제된다")
    @Test
    void deleteUserWithoutFeedback(){
        // given
        User addedUser = User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("junbeom")
                .build();
        userRepository.save(addedUser);

        Long targetUserId = addedUser.getId();

        // when
        Long deletedUserId = userAuthService.deleteUser(targetUserId, null);

        // then
        assertThat(deletedUserId).isEqualTo(targetUserId);
        assertThat(userRepository.findById(targetUserId)).isEmpty();
        assertThat(accountDeletionFeedbackRepository.findAll()).isEmpty();
    }

    @DisplayName("소셜 계정 삭제 시 사용자 소유 데이터는 삭제하고 탈퇴 피드백만 익명화해 보존한다")
    @ParameterizedTest
    @EnumSource(SocialType.class)
    void deleteSocialUserRemovesAssociatedDataAndRetainsAnonymizedDeletionFeedback(SocialType socialType) {
        // given
        User targetUser = User.builder()
                .email(socialType.name().toLowerCase() + "@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("delete-target-" + socialType.name().toLowerCase())
                .role(Role.USER)
                .socialType(socialType)
                .socialId("social-id-" + socialType.name().toLowerCase())
                .firebaseToken("firebase-token")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .socialLoginToken("provider-refresh-token")
                .build();
        UserSetting userSetting = UserSetting.builder()
                .userSettingId(UUID.randomUUID())
                .user(targetUser)
                .build();
        targetUser.setUserSetting(userSetting);
        userRepository.saveAndFlush(targetUser);

        User friendUser = userRepository.saveAndFlush(User.builder()
                .email("friend-" + socialType.name().toLowerCase() + "@example.com")
                .password(passwordEncoder.encode("password1234"))
                .name("friend-" + socialType.name().toLowerCase())
                .role(Role.USER)
                .build());

        Place place = placeRepository.save(Place.builder()
                .placeId(UUID.randomUUID())
                .placeName("Office")
                .build());
        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .scheduleId(UUID.randomUUID())
                .user(targetUser)
                .place(place)
                .scheduleName("Release check")
                .moveTime(20)
                .scheduleTime(LocalDateTime.of(2026, 5, 9, 10, 0))
                .isChange(false)
                .isStarted(false)
                .doneStatus(DoneStatus.NOT_ENDED)
                .scheduleSpareTime(10)
                .latenessTime(null)
                .scheduleNote("Delete cascade verification")
                .build());
        preparationScheduleRepository.save(PreparationSchedule.builder()
                .preparationScheduleId(UUID.randomUUID())
                .schedule(schedule)
                .preparationName("Pack")
                .preparationTime(5)
                .build());
        notificationScheduleRepository.save(NotificationSchedule.builder()
                .schedule(schedule)
                .notificationTime(LocalDateTime.of(2026, 5, 9, 9, 45))
                .isSent(false)
                .build());
        preparationUserRepository.save(PreparationUser.builder()
                .preparationUserId(UUID.randomUUID())
                .user(targetUser)
                .preparationName("Brush teeth")
                .preparationTime(3)
                .build());
        feedbackRepository.save(Feedback.builder()
                .feedbackId(UUID.randomUUID())
                .user(targetUser)
                .message("general feedback")
                .createAt(LocalDateTime.of(2026, 5, 9, 8, 0))
                .build());
        friendshipRepository.save(FriendShip.builder()
                .friendShipId(UUID.randomUUID())
                .requesterId(targetUser.getId())
                .receiverId(friendUser.getId())
                .acceptStatus("ACCEPTED")
                .build());
        userAlarmSettingRepository.save(UserAlarmSetting.defaultFor(targetUser));
        UserDevice userDevice = UserDevice.create(targetUser, "device-" + socialType.name().toLowerCase());
        userDevice.activate("ios", "1.0.0", "17.0", true, "native", "fcm", Instant.now());
        userDevice.bindSession("session-access-token", "session-refresh-token");
        userDevice.updateFirebaseToken("device-firebase-token");
        userDeviceRepository.save(userDevice);
        UserAlarmStatus alarmStatus = UserAlarmStatus.create(targetUser, userDevice);
        alarmStatus.replace(
                Instant.now(),
                LocalDateTime.of(2026, 5, 9, 0, 0),
                LocalDateTime.of(2026, 5, 10, 0, 0),
                LocalDateTime.of(2026, 5, 9, 9, 30),
                LocalDateTime.of(2026, 5, 9, 10, 0),
                "READY",
                null,
                "native",
                "fcm",
                1,
                "[\"" + schedule.getScheduleId() + "\"]",
                0,
                null
        );
        userAlarmStatusRepository.save(alarmStatus);

        UUID deletionFeedbackId = UUID.randomUUID();
        FeedbackAddDto feedbackAddDto = FeedbackAddDto.builder()
                .feedbackId(deletionFeedbackId)
                .message("delete feedback")
                .build();

        // when
        Long deletedUserId = userAuthService.deleteUser(targetUser.getId(), feedbackAddDto);

        // then
        assertThat(deletedUserId).isEqualTo(targetUser.getId());
        assertThat(userRepository.findById(targetUser.getId())).isEmpty();
        assertThat(userRepository.findById(friendUser.getId())).isPresent();
        assertThat(scheduleRepository.findById(schedule.getScheduleId())).isEmpty();
        assertThat(preparationScheduleRepository.count()).isZero();
        assertThat(notificationScheduleRepository.count()).isZero();
        assertThat(preparationUserRepository.count()).isZero();
        assertThat(feedbackRepository.count()).isZero();
        assertThat(friendshipRepository.count()).isZero();
        assertThat(userSettingRepository.findByUserId(targetUser.getId())).isEmpty();
        assertThat(userAlarmSettingRepository.findByUserId(targetUser.getId())).isEmpty();
        assertThat(userDeviceRepository.findByUserIdAndDeviceId(targetUser.getId(), userDevice.getDeviceId())).isEmpty();
        assertThat(userAlarmStatusRepository.findByUserDeviceUserDeviceId(userDevice.getUserDeviceId())).isEmpty();

        AccountDeletionFeedback deletionFeedback = accountDeletionFeedbackRepository.findById(deletionFeedbackId)
                .orElseThrow();
        assertThat(deletionFeedback.getDeletedUserId()).isEqualTo(targetUser.getId());
        assertThat(deletionFeedback.getSocialType()).isEqualTo(socialType);
        assertThat(deletionFeedback.getMessage()).isEqualTo("delete feedback");
        assertThat(deletionFeedback.getEmailHash()).hasSize(64);
        assertThat(deletionFeedback.getEmailHash()).doesNotContain(targetUser.getEmail());
        assertThat(deletionFeedback.getCreatedAt()).isNotNull();
    }



    private UserSignUpDto getUserSignUpDto(String email, String password, String name) {
        UserSignUpDto userSignUpDto = UserSignUpDto.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
        return userSignUpDto;
    }
}
