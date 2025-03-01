package devkor.ontime_back.service;

import devkor.ontime_back.dto.ChangePasswordDto;
import devkor.ontime_back.dto.UserAdditionalInfoDto;
import devkor.ontime_back.dto.UserSignUpDto;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.PreparationScheduleRepository;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.repository.UserSettingRepository;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserAuthService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final PreparationUserRepository preparationUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PreparationScheduleRepository preparationScheduleRepository;

    // 엑세스토큰에서 UserId 추출
    public Long getUserIdFromToken(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization").substring(7); // "Bearer "를 제외한 토큰
        String refreshToken = request.getHeader("refresh-token");
        return jwtTokenProvider.extractUserId(accessToken).orElseThrow(() -> new RuntimeException("User ID not found in token"));
    }

    // 자체 로그인 회원가입
    @Transactional
    public User signUp(HttpServletRequest request, HttpServletResponse response, UserSignUpDto userSignUpDto) throws Exception {

        if (userRepository.findByEmail(userSignUpDto.getEmail()).isPresent()) {
            throw new GeneralException(ErrorCode.EMAIL_ALREADY_EXIST);
        }

        if (userRepository.findByName(userSignUpDto.getName()).isPresent()) {
            throw new GeneralException(ErrorCode.NAME_ALREADY_EXIST);
        }

        if (userSettingRepository.findByUserSettingId(userSignUpDto.getUserSettingId()).isPresent()) {
            throw new GeneralException(ErrorCode.USER_SETTING_ALREADY_EXIST);
        }

        // 자체 로그인시, USER로 설정
        User user = User.builder()
                .email(userSignUpDto.getEmail())
                .password(userSignUpDto.getPassword())
                .name(userSignUpDto.getName())
                .role(Role.GUEST)
                .punctualityScore((float)-1)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build();
        // 비밀번호 암호화 후 저장
        user.passwordEncode(passwordEncoder);

        // 사용자 앱 설정 세팅(pk와 fk만 세팅, 나머지는 디폴트설정값(엔티티에 정의)으로 생성됨)
        UserSetting userSetting = UserSetting.builder()
                .userSettingId(userSignUpDto.getUserSettingId())
                .user(user)
                .build();

        user.setUserSetting(userSetting);
        userRepository.save(user); //CASCADE옵션 덕분에 userRepository만 save해주면 됨(userSettingRepository는 save안해줘도 부모인 user를 따라 저장됨)

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);

        user.updateRefreshToken(refreshToken);
        userRepository.saveAndFlush(user);

        return user;
    }

    @Transactional
    public User addInfo(Long id, UserAdditionalInfoDto userAdditionalInfoDto) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 유저 id입니다."));
        user.updateSpareTime(userAdditionalInfoDto.getSpareTime());
        user.updateNote(userAdditionalInfoDto.getNote());
        userRepository.save(user);

        return user;
    }

    @Transactional
    public User changePassword(Long userId, ChangePasswordDto changePasswordDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 유저 id입니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new GeneralException(ErrorCode.PASSWORD_INCORRECT);
        }

        // 새로운 비밀번호와 현재 비밀번호 비교
        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), user.getPassword())) {
            throw new GeneralException(ErrorCode.SAME_PASSWORD);
        }

        // 새로운 비밀번호 저장
        user.updatePassword(changePasswordDto.getNewPassword(), passwordEncoder);
        userRepository.save(user);

        return user;
    }

    @Transactional
    public Long deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        userRepository.delete(user);

        return userId;
    }

}