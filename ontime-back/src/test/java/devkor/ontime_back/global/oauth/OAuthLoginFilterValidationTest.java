package devkor.ontime_back.global.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import devkor.ontime_back.dto.AppleTokenResponseDto;
import devkor.ontime_back.dto.OAuthAppleUserDto;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.global.oauth.apple.AppleLoginFilter;
import devkor.ontime_back.global.oauth.apple.AppleLoginService;
import devkor.ontime_back.global.oauth.google.GoogleLoginFilter;
import devkor.ontime_back.global.oauth.google.GoogleLoginService;
import devkor.ontime_back.global.oauth.kakao.KakaoLoginFilter;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.AnalyticsPreferenceService;
import devkor.ontime_back.service.AuthTokenService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginFilterValidationTest {

    @Mock
    private GoogleLoginService googleLoginService;

    @Mock
    private AppleLoginService appleLoginService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAlarmSettingRepository userAlarmSettingRepository;

    @Mock
    private AnalyticsPreferenceService analyticsPreferenceService;

    @Mock
    private AuthTokenService authTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("구글 로그인 필터가 잘못된 요청을 400 validation 응답으로 처리한다")
    void googleLoginFilterRejectsInvalidRequest() throws Exception {
        GoogleLoginFilter filter = new GoogleLoginFilter(
                "/oauth2/google/login",
                objectMapper,
                validator,
                googleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/google/login", "{}"),
                response))
                .isInstanceOf(AuthenticationException.class);

        assertValidationResponse(response);
        verifyNoInteractions(googleLoginService, userRepository);
    }

    @Test
    @DisplayName("구글 로그인 필터가 검증 실패한 idToken을 인증 실패로 처리한다")
    void googleLoginFilterRejectsUnverifiedIdToken() throws Exception {
        GoogleLoginFilter filter = new GoogleLoginFilter(
                "/oauth2/google/login",
                objectMapper,
                validator,
                googleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(googleLoginService.verifyIdentityToken("invalid-token")).thenReturn(null);

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/google/login", "{\"idToken\":\"invalid-token\"}"),
                response))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid Google identity token");

        verify(googleLoginService).verifyIdentityToken("invalid-token");
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("구글 로그인 필터가 subject 없는 검증 토큰을 인증 실패로 처리한다")
    void googleLoginFilterRejectsVerifiedTokenWithoutSubject() throws Exception {
        GoogleLoginFilter filter = new GoogleLoginFilter(
                "/oauth2/google/login",
                objectMapper,
                validator,
                googleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("user@example.com");

        when(googleLoginService.verifyIdentityToken("valid-token")).thenReturn(payload);

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/google/login", "{\"idToken\":\"valid-token\"}"),
                response))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Google identity token has no subject");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("구글 로그인 필터가 검증된 토큰의 기존 유저를 로그인 처리한다")
    void googleLoginFilterLogsInExistingUser() throws Exception {
        GoogleLoginFilter filter = new GoogleLoginFilter(
                "/oauth2/google/login",
                objectMapper,
                validator,
                googleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-id");
        payload.setEmail("user@example.com");
        User existingUser = user(1L, "user@example.com", Role.USER);

        when(googleLoginService.verifyIdentityToken("valid-token")).thenReturn(payload);
        when(userRepository.findAllBySocialTypeAndSocialIdOrderByIdDesc(SocialType.GOOGLE, "google-id"))
                .thenReturn(java.util.List.of(existingUser));
        when(googleLoginService.handleLogin(any(), any(), any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(existingUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/google/login", "{\"idToken\":\"valid-token\",\"refreshToken\":\"google-refresh\"}"),
                response).getPrincipal()).isSameAs(existingUser);

        verify(googleLoginService).handleLogin(any(), any(), any());
    }

    @Test
    @DisplayName("구글 로그인 필터가 신규 유저 정보를 검증된 토큰에서 만들어 회원가입 처리한다")
    void googleLoginFilterRegistersNewUserFromVerifiedPayload() throws Exception {
        GoogleLoginFilter filter = new GoogleLoginFilter(
                "/oauth2/google/login",
                objectMapper,
                validator,
                googleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-id");
        payload.setEmail("new@example.com");
        payload.put("name", "New User");
        payload.put("picture", "https://example.com/picture.png");
        User newUser = user(2L, "new@example.com", Role.GUEST);

        when(googleLoginService.verifyIdentityToken("valid-token")).thenReturn(payload);
        when(userRepository.findAllBySocialTypeAndSocialIdOrderByIdDesc(SocialType.GOOGLE, "google-id"))
                .thenReturn(java.util.List.of());
        when(googleLoginService.handleRegister(any(), any(), any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(newUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/google/login", "{\"idToken\":\"valid-token\",\"refreshToken\":\"google-refresh\"}"),
                response).getPrincipal()).isSameAs(newUser);

        verify(googleLoginService).handleRegister(any(), any(), any());
    }

    @Test
    @DisplayName("구글 회원가입 중 중복 socialId가 생기면 방금 생성된 기존 계정으로 로그인한다")
    void googleLoginFilterFallsBackToLoginWhenConcurrentRegisterCreatesSameSocialUser() throws Exception {
        GoogleLoginFilter filter = new GoogleLoginFilter(
                "/oauth2/google/login",
                objectMapper,
                validator,
                googleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-id");
        payload.setEmail("new@example.com");
        payload.put("name", "New User");
        payload.put("picture", "https://example.com/picture.png");
        User concurrentlyCreatedUser = user(3L, "new@example.com", Role.GUEST);

        when(googleLoginService.verifyIdentityToken("valid-token")).thenReturn(payload);
        when(userRepository.findAllBySocialTypeAndSocialIdOrderByIdDesc(SocialType.GOOGLE, "google-id"))
                .thenReturn(java.util.List.of())
                .thenReturn(java.util.List.of(concurrentlyCreatedUser));
        when(googleLoginService.handleRegister(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate social id"));
        when(googleLoginService.handleLogin(any(), any(), any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(concurrentlyCreatedUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/google/login", "{\"idToken\":\"valid-token\",\"refreshToken\":\"google-refresh\"}"),
                response).getPrincipal()).isSameAs(concurrentlyCreatedUser);

        verify(googleLoginService).handleRegister(any(), any(), any());
        verify(googleLoginService).handleLogin(any(), any(), any());
    }

    @Test
    @DisplayName("카카오 로그인 필터가 잘못된 요청을 400 validation 응답으로 처리한다")
    void kakaoLoginFilterRejectsInvalidRequest() throws Exception {
        KakaoLoginFilter filter = new KakaoLoginFilter(
                "/oauth2/kakao/login",
                objectMapper,
                validator,
                jwtTokenProvider,
                userRepository,
                userAlarmSettingRepository,
                analyticsPreferenceService,
                authTokenService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/kakao/login", "{\"id\":\"\",\"profile\":{}}"),
                response))
                .isInstanceOf(AuthenticationException.class);

        assertValidationResponse(response);
        verifyNoInteractions(jwtTokenProvider, userRepository, userAlarmSettingRepository, analyticsPreferenceService);
    }

    @Test
    @DisplayName("카카오 로그인 필터가 기존 소셜 유저의 애플리케이션 토큰을 재발급한다")
    void kakaoLoginFilterLogsInExistingUser() throws Exception {
        KakaoLoginFilter filter = kakaoLoginFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        User existingUser = user(1L, "user@example.com", Role.USER);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao-id"))
                .thenReturn(Optional.of(existingUser));
        when(authTokenService.issueLoginTokens(existingUser, response)).thenAnswer(invocation -> {
            existingUser.updateAccessToken("access-token");
            existingUser.updateRefreshToken("refresh-token");
            return new AuthTokenService.AuthTokens("access-token", "refresh-token");
        });

        assertThat(filter.attemptAuthentication(
                request("/oauth2/kakao/login", validKakaoBody()),
                response).getPrincipal()).isSameAs(existingUser);

        assertThat(existingUser.getAccessToken()).isEqualTo("access-token");
        assertThat(existingUser.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getContentAsString()).contains("로그인에 성공하였습니다.");
        verify(authTokenService).issueLoginTokens(existingUser, response);
        verify(userRepository).saveAndFlush(existingUser);
    }

    @Test
    @DisplayName("카카오 로그인 필터가 신규 소셜 유저와 기본 알람 설정을 생성한다")
    void kakaoLoginFilterRegistersNewGuestUser() throws Exception {
        KakaoLoginFilter filter = kakaoLoginFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        User savedUser = user(2L, "kakao@example.com", Role.GUEST);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao-id"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authTokenService.issueLoginTokens(savedUser, response)).thenAnswer(invocation -> {
            savedUser.updateAccessToken("access-token");
            savedUser.updateRefreshToken("refresh-token");
            return new AuthTokenService.AuthTokens("access-token", "refresh-token");
        });

        assertThat(filter.attemptAuthentication(
                request("/oauth2/kakao/login", validKakaoBody()),
                response).getPrincipal()).isNotNull();

        assertThat(savedUser.getAccessToken()).isEqualTo("access-token");
        assertThat(savedUser.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getContentAsString()).contains("온보딩이 필요합니다.");
        verify(authTokenService).issueLoginTokens(savedUser, response);
        verify(userRepository, times(2)).save(any(User.class));
        verify(userAlarmSettingRepository).save(any(UserAlarmSetting.class));
    }

    @Test
    @DisplayName("애플 로그인 필터가 잘못된 요청을 400 validation 응답으로 처리한다")
    void appleLoginFilterRejectsInvalidRequest() throws Exception {
        AppleLoginFilter filter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                validator,
                appleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/apple/login", "{\"idToken\":\"\",\"authCode\":\"\",\"fullName\":\"\"}"),
                response))
                .isInstanceOf(AuthenticationException.class);

        assertValidationResponse(response);
        verifyNoInteractions(appleLoginService, userRepository);
    }

    @Test
    @DisplayName("애플 로그인 필터가 기존 유저는 Apple token 교환 없이 로그인 처리한다")
    void appleLoginFilterLogsInExistingUser() throws Exception {
        AppleLoginFilter filter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                validator,
                appleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = Jwts.claims().setSubject("apple-id");
        claims.put("email", "user@example.com");
        User existingUser = user(1L, "user@example.com", Role.USER);

        when(appleLoginService.verifyIdentityToken("apple-id-token")).thenReturn(claims);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "apple-id"))
                .thenReturn(Optional.of(existingUser));
        when(appleLoginService.handleLogin(null, existingUser, response)).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(existingUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/apple/login", validAppleBody()),
                response).getPrincipal()).isSameAs(existingUser);

        verify(appleLoginService, never()).getAppleAccessTokenAndRefreshToken("auth-code");
        verify(appleLoginService).handleLogin(null, existingUser, response);
    }

    @Test
    @DisplayName("애플 로그인 필터가 기존 유저 경로에서는 실패 가능한 Apple token 교환을 시도하지 않는다")
    void appleLoginFilterSkipsTokenExchangeForExistingUser() throws Exception {
        AppleLoginFilter filter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                validator,
                appleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = Jwts.claims().setSubject("apple-id");
        User existingUser = user(1L, "user@example.com", Role.USER);

        when(appleLoginService.verifyIdentityToken("apple-id-token")).thenReturn(claims);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "apple-id"))
                .thenReturn(Optional.of(existingUser));
        when(appleLoginService.handleLogin(null, existingUser, response)).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(existingUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/apple/login", validAppleBody()),
                response).getPrincipal()).isSameAs(existingUser);

        verify(appleLoginService, never()).getAppleAccessTokenAndRefreshToken("auth-code");
        verify(appleLoginService).handleLogin(null, existingUser, response);
    }

    @Test
    @DisplayName("애플 로그인 필터가 신규 유저를 Apple identity token과 auth code로 회원가입 처리한다")
    void appleLoginFilterRegistersNewUser() throws Exception {
        AppleLoginFilter filter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                validator,
                appleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = Jwts.claims().setSubject("apple-id");
        AppleTokenResponseDto tokenResponse = appleTokenResponse("apple-refresh-token");
        User newUser = user(2L, "new@example.com", Role.GUEST);

        when(appleLoginService.verifyIdentityToken("apple-id-token")).thenReturn(claims);
        when(appleLoginService.getAppleAccessTokenAndRefreshToken("auth-code")).thenReturn(tokenResponse);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "apple-id"))
                .thenReturn(Optional.empty());
        when(appleLoginService.handleRegister(any(), any(), any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(newUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/apple/login", validAppleBody()),
                response).getPrincipal()).isSameAs(newUser);

        verify(appleLoginService).handleRegister(any(), any(), any());
    }

    @Test
    @DisplayName("애플 로그인 필터가 요청 email이 비어 있으면 identity token email로 신규 유저를 생성한다")
    void appleLoginFilterUsesIdentityTokenEmailWhenRequestEmailIsMissing() throws Exception {
        AppleLoginFilter filter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                validator,
                appleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = Jwts.claims().setSubject("apple-id");
        claims.put("email", "token@example.com");
        AppleTokenResponseDto tokenResponse = appleTokenResponse("apple-refresh-token");
        User newUser = user(2L, "token@example.com", Role.GUEST);

        when(appleLoginService.verifyIdentityToken("apple-id-token")).thenReturn(claims);
        when(appleLoginService.getAppleAccessTokenAndRefreshToken("auth-code")).thenReturn(tokenResponse);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "apple-id"))
                .thenReturn(Optional.empty());
        when(appleLoginService.handleRegister(eq("apple-refresh-token"), any(), any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(newUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/apple/login", appleBodyWithoutEmail()),
                response).getPrincipal()).isSameAs(newUser);

        verify(appleLoginService).handleRegister(eq("apple-refresh-token"), org.mockito.ArgumentMatchers.argThat(
                (OAuthAppleUserDto userDto) -> "token@example.com".equals(userDto.getEmail())
        ), eq(response));
    }

    @Test
    @DisplayName("애플 로그인 필터가 Apple refresh token 교환 실패 시 identity token 검증만으로 신규 유저를 회원가입 처리한다")
    void appleLoginFilterRegistersNewUserWhenTokenExchangeFails() throws Exception {
        AppleLoginFilter filter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                validator,
                appleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = Jwts.claims().setSubject("apple-id");
        User newUser = user(2L, "new@example.com", Role.GUEST);

        when(appleLoginService.verifyIdentityToken("apple-id-token")).thenReturn(claims);
        when(appleLoginService.getAppleAccessTokenAndRefreshToken("auth-code"))
                .thenThrow(new RuntimeException("invalid_client"));
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "apple-id"))
                .thenReturn(Optional.empty());
        when(appleLoginService.handleRegister(eq(null), any(), any())).thenReturn(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(newUser, null)
        );

        assertThat(filter.attemptAuthentication(
                request("/oauth2/apple/login", validAppleBody()),
                response).getPrincipal()).isSameAs(newUser);

        verify(appleLoginService).handleRegister(eq(null), any(), any());
    }

    private KakaoLoginFilter kakaoLoginFilter() {
        return new KakaoLoginFilter(
                "/oauth2/kakao/login",
                objectMapper,
                validator,
                jwtTokenProvider,
                userRepository,
                userAlarmSettingRepository,
                analyticsPreferenceService,
                authTokenService);
    }

    private MockHttpServletRequest request(String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.setRequestURI(uri);
        request.setContent(body.getBytes());
        return request;
    }

    private void assertValidationResponse(MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("\"status\":\"error\"");
        assertThat(response.getContentAsString()).contains("\"code\":1002");
        assertThat(response.getContentAsString()).contains("\"errors\"");
    }

    private String validKakaoBody() {
        return """
                {
                  "id": "kakao-id",
                  "profile": {
                    "nickname": "Kakao User",
                    "profile_image_url": "https://example.com/profile.png"
                  }
                }
                """;
    }

    private String validAppleBody() {
        return """
                {
                  "idToken": "apple-id-token",
                  "authCode": "auth-code",
                  "fullName": "Apple User",
                  "email": "new@example.com"
                }
                """;
    }

    private String appleBodyWithoutEmail() {
        return """
                {
                  "idToken": "apple-id-token",
                  "authCode": "auth-code",
                  "fullName": "Apple User"
                }
                """;
    }

    private AppleTokenResponseDto appleTokenResponse(String refreshToken) {
        AppleTokenResponseDto response = new AppleTokenResponseDto();
        ReflectionTestUtils.setField(response, "refreshToken", refreshToken);
        return response;
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .name("Kakao User")
                .spareTime(10)
                .note("note")
                .punctualityScore(95.0f)
                .role(role)
                .socialType(SocialType.KAKAO)
                .socialId("kakao-id")
                .build();
    }
}
