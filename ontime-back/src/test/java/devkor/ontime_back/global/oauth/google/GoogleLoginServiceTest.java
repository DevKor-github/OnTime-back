package devkor.ontime_back.global.oauth.google;

import devkor.ontime_back.dto.OAuthGoogleRequestDto;
import devkor.ontime_back.dto.OAuthGoogleUserDto;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.AnalyticsPreferenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleLoginServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAlarmSettingRepository userAlarmSettingRepository;

    @Mock
    private AnalyticsPreferenceService analyticsPreferenceService;

    private GoogleLoginService googleLoginService;
    @Mock
    private RestTemplate revokeRestTemplate;

    @BeforeEach
    void setUp() {
        googleLoginService = new GoogleLoginService(
                jwtTokenProvider,
                userRepository,
                userAlarmSettingRepository,
                analyticsPreferenceService,
                "web-client.apps.googleusercontent.com",
                "ios-client.apps.googleusercontent.com, android-client.apps.googleusercontent.com",
                revokeRestTemplate
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handleLoginRotatesApplicationTokensAndWritesLoginResponse() throws Exception {
        User user = user(1L, "user@example.com", "Existing User", Role.USER);
        OAuthGoogleRequestDto request = googleRequest("google-refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.createAccessToken("user@example.com", 1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");

        Authentication authentication = googleLoginService.handleLogin(request, user, response);

        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(user.getSocialLoginToken()).isEqualTo("google-refresh-token");
        assertThat(user.getAccessToken()).isEqualTo("access-token");
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"message\": \"로그인에 성공하였습니다.\"");
        verify(jwtTokenProvider).sendAccessAndRefreshToken(response, "access-token", "refresh-token");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void handleRegisterCreatesGuestUserSettingsAndDefaultAlarmSettings() throws Exception {
        OAuthGoogleRequestDto request = googleRequest("google-refresh-token");
        OAuthGoogleUserDto googleUser = new OAuthGoogleUserDto(
                "google-id",
                "New User",
                "https://example.com/profile.png",
                "new@example.com"
        );
        User savedUser = user(2L, "new@example.com", "New User", Role.GUEST);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.createAccessToken("new@example.com", 2L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication authentication = googleLoginService.handleRegister(request, googleUser, response);

        assertThat(authentication.getPrincipal()).isSameAs(savedUser);
        assertThat(savedUser.getAccessToken()).isEqualTo("access-token");
        assertThat(savedUser.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getContentAsString()).contains("회원가입에 성공하였습니다.");
        verify(jwtTokenProvider).createAccessToken("new@example.com", 2L);
        verify(userRepository, times(2)).save(any(User.class));
        verify(userAlarmSettingRepository).save(any(UserAlarmSetting.class));
        verify(analyticsPreferenceService).createDefaultPreference(savedUser);
    }

    @Test
    void constructorTrimsAndFiltersAllowedGoogleAudienceClientIds() {
        GoogleLoginService service = new GoogleLoginService(
                jwtTokenProvider,
                userRepository,
                userAlarmSettingRepository,
                analyticsPreferenceService,
                "web-client.apps.googleusercontent.com",
                " ios-client.apps.googleusercontent.com, ,android-client.apps.googleusercontent.com "
        );

        @SuppressWarnings("unchecked")
        List<String> validClientIds = (List<String>) ReflectionTestUtils.getField(service, "validClientIds");

        assertThat(validClientIds).containsExactly(
                "web-client.apps.googleusercontent.com",
                "ios-client.apps.googleusercontent.com",
                "android-client.apps.googleusercontent.com"
        );
    }

    @Test
    void verifyIdentityTokenRejectsMalformedCredentialsBeforeAuthentication() {
        assertThatThrownBy(() -> googleLoginService.verifyIdentityToken("not-a-google-jwt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revokeTokenPostsTheStoredGoogleRefreshTokenAndReturnsSuccessFor2xxResponse() {
        User user = user(4L, "user@example.com", "Existing User", Role.USER);
        user.updateSocialLoginToken("google-refresh-token");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(revokeRestTemplate.exchange(
                eq("https://oauth2.googleapis.com/revoke?token=google-refresh-token"),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(""));

        boolean revoked = googleLoginService.revokeToken(4L);

        assertThat(revoked).isTrue();
    }

    @Test
    void revokeTokenFailsWhenTheUserDoesNotExist() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> googleLoginService.revokeToken(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found with id: 404");
    }

    @Test
    void googleClaimDiagnosticsHandleParseableAndMalformedTokensWithoutExposingFullClientId() {
        String parseableToken = fakeGoogleToken("""
                {
                  "aud": "web-client.apps.googleusercontent.com",
                  "azp": "android-client.apps.googleusercontent.com",
                  "iss": "https://accounts.google.com",
                  "exp": %d
                }
                """.formatted(Instant.now().plusSeconds(60).getEpochSecond()));

        ReflectionTestUtils.invokeMethod(googleLoginService, "logGoogleIdentityTokenClaims", parseableToken);
        ReflectionTestUtils.invokeMethod(googleLoginService, "logGoogleIdentityTokenClaims", "malformed-token");

        assertThat((String) ReflectionTestUtils.invokeMethod(googleLoginService, "maskClientId", "invalidclient"))
                .isEqualTo("<invalid-client-id>");
    }

    private String fakeGoogleToken(String payloadJson) {
        return base64Url("{\"alg\":\"RS256\",\"kid\":\"test\"}")
                + "."
                + base64Url(payloadJson)
                + ".signature";
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private OAuthGoogleRequestDto googleRequest(String refreshToken) {
        OAuthGoogleRequestDto request = new OAuthGoogleRequestDto();
        ReflectionTestUtils.setField(request, "idToken", "id-token");
        ReflectionTestUtils.setField(request, "refreshToken", refreshToken);
        return request;
    }

    private User user(Long id, String email, String name, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .spareTime(10)
                .note("note")
                .punctualityScore(95.0f)
                .role(role)
                .socialType(SocialType.GOOGLE)
                .socialId("google-id")
                .build();
    }
}
