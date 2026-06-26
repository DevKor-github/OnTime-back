package devkor.ontime_back.global.oauth.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.OAuthKakaoUserDto;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ValidationErrorWriter;
import devkor.ontime_back.service.AnalyticsPreferenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class KakaoLoginFilter extends AbstractAuthenticationProcessingFilter {

    private final UserRepository userRepository;
    private final UserAlarmSettingRepository userAlarmSettingRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AnalyticsPreferenceService analyticsPreferenceService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public KakaoLoginFilter(String defaultFilterProcessesUrl,
                            ObjectMapper objectMapper,
                            Validator validator,
                            JwtTokenProvider jwtTokenProvider,
                            UserRepository userRepository,
                            UserAlarmSettingRepository userAlarmSettingRepository,
                            AnalyticsPreferenceService analyticsPreferenceService) {
        super(defaultFilterProcessesUrl);
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userAlarmSettingRepository = userAlarmSettingRepository;
        this.analyticsPreferenceService = analyticsPreferenceService;

    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        OAuthKakaoUserDto oAuthKakaoUserDto = objectMapper.readValue(request.getInputStream(), OAuthKakaoUserDto.class);
        Set<ConstraintViolation<OAuthKakaoUserDto>> violations = validator.validate(oAuthKakaoUserDto);
        if (!violations.isEmpty()) {
            ValidationErrorWriter.write(response, objectMapper, violations);
            throw new RequestValidationException();
        }

        Optional<User> existingUser = userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, oAuthKakaoUserDto.getId());

        if (existingUser.isPresent()) {
            return handleLogin(existingUser.get(), response);
        } else {
            return handleRegister(oAuthKakaoUserDto, response);
        }
    }

    private Authentication handleLogin(User user, HttpServletResponse response) throws IOException {
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        user.updateAccessToken(accessToken);
        user.updateRefreshToken(refreshToken);
        userRepository.saveAndFlush(user);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String msg = user.getRole().name().equals("GUEST") ? "유저의 ROLE이 GUEST이므로 온보딩API를 호출해 온보딩을 진행해야합니다." : "로그인에 성공하였습니다.";
        // JSON 응답 생성
        String responseBody = String.format(
                "{ \"status\": \"success\", \"code\": \"200\", \"message\": \"%s\", \"data\": { " +
                        "\"userId\": %d, \"email\": \"%s\", \"name\": \"%s\", " +
                        "\"spareTime\": \"%s\", \"note\": \"%s\", \"punctualityScore\": %f, \"role\": \"%s\" } }",
                msg, user.getId(), user.getEmail(), user.getName(),
                user.getSpareTime(), user.getNote(), user.getPunctualityScore(), user.getRole().name()
        );

        response.getWriter().write(responseBody);
        response.getWriter().flush();

        return new UsernamePasswordAuthenticationToken(user, null, Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())));
    }

    private Authentication handleRegister(OAuthKakaoUserDto oAuthKakaoUserDto, HttpServletResponse response) throws IOException {
        User newUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId(oAuthKakaoUserDto.getId())
                .name(oAuthKakaoUserDto.getProfile().getNickname())
                .imageUrl(oAuthKakaoUserDto.getProfile().getProfileImageUrl())
                .role(Role.GUEST)
                .build();

        UserSetting userSetting = UserSetting.builder()
                .userSettingId(UUID.randomUUID())
                .user(newUser)
                .build();
        newUser.setUserSetting(userSetting);

        User savedUser = userRepository.save(newUser);

        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail(), savedUser.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();
        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        savedUser.updateAccessToken(accessToken);
        savedUser.updateRefreshToken(refreshToken);
        userRepository.save(savedUser);
        userAlarmSettingRepository.save(UserAlarmSetting.defaultFor(savedUser));
        analyticsPreferenceService.createDefaultPreference(savedUser);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String responseBody = String.format(
                "{\"message\": \"%s\", \"role\": \"%s\"}",
                "회원가입이 완료되었습니다. ROLE이 GUEST이므로 온보딩이 필요합니다.",
                savedUser.getRole().name()
        );

        response.getWriter().write(responseBody);
        response.getWriter().flush();

        return new UsernamePasswordAuthenticationToken(newUser, null, Collections.singletonList(new SimpleGrantedAuthority(newUser.getRole().name())));
    }


    // 인증 성공 처리
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {

        log.info("카카오 로그인 성공");
        SecurityContextHolder.getContext().setAuthentication(authResult);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // 인증 실패 처리
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        if (failed instanceof RequestValidationException) {
            return;
        }
        log.warn("카카오 로그인 실패");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"status\":\"error\", \"message\":\"Authentication failed\"}");
    }

    private static class RequestValidationException extends AuthenticationException {
        private RequestValidationException() {
            super("Request validation failed");
        }
    }
}
