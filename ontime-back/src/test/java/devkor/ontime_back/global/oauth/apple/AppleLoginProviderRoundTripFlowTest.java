package devkor.ontime_back.global.oauth.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.global.jwt.JwtUtils;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.AnalyticsPreferenceService;
import devkor.ontime_back.service.AuthTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleLoginProviderRoundTripFlowTest {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";

    @Mock
    private ApplePublicKeyGenerator applePublicKeyGenerator;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAlarmSettingRepository userAlarmSettingRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AnalyticsPreferenceService analyticsPreferenceService;

    @Mock
    private AuthTokenService authTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate appleRestTemplate = org.mockito.Mockito.mock(RestTemplate.class);
    private final ApplePublicKeyResponse appleKeys = new ApplePublicKeyResponse(java.util.List.of());
    private PublicKey applePublicKey;
    private AppleLoginService appleLoginService;
    private AppleLoginFilter appleLoginFilter;

    @BeforeEach
    void setUp() throws Exception {
        applePublicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        appleLoginService = new AppleLoginService(
                applePublicKeyGenerator,
                jwtUtils,
                userRepository,
                userAlarmSettingRepository,
                jwtTokenProvider,
                analyticsPreferenceService,
                authTokenService
        );
        ReflectionTestUtils.setField(appleLoginService, "restTemplate", appleRestTemplate);
        ReflectionTestUtils.setField(appleLoginService, "clientId", "com.ontime.service");
        configureAppleClientSecretInputs();

        appleLoginFilter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                Validation.buildDefaultValidatorFactory().getValidator(),
                appleLoginService,
                userRepository
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void coldCacheExistingUserThenDifferentExistingUserFetchesJwksOnlyOnceAndNeverExchangesCredential() throws Exception {
        prepareIdentityVerification("existing-id-token", "existing-apple-id");
        prepareIdentityVerification("other-id-token", "other-apple-id");
        User existingUser = appleUser(1L, "existing-apple-id", "existing@example.com", Role.USER);
        User otherUser = appleUser(2L, "other-apple-id", "other@example.com", Role.USER);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "existing-apple-id"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "other-apple-id"))
                .thenReturn(Optional.of(otherUser));
        when(authTokenService.issueLoginTokens(any(User.class), any())).thenReturn(
                new AuthTokenService.AuthTokens("access-token", "refresh-token")
        );

        appleLoginFilter.attemptAuthentication(
                appleRequest("existing-id-token", "existing-auth-code", "Existing User", "existing@example.com"),
                new MockHttpServletResponse());
        appleLoginFilter.attemptAuthentication(
                appleRequest("other-id-token", "other-auth-code", "Other User", "other@example.com"),
                new MockHttpServletResponse());

        verify(appleRestTemplate, times(1)).getForObject(APPLE_KEYS_URL, ApplePublicKeyResponse.class);
        verify(appleRestTemplate, times(0)).exchange(eq(APPLE_TOKEN_URL), eq(HttpMethod.POST), any(), eq(JsonNode.class));
        verify(userRepository).findBySocialTypeAndSocialId(SocialType.APPLE, "existing-apple-id");
        verify(userRepository).findBySocialTypeAndSocialId(SocialType.APPLE, "other-apple-id");
    }

    @Test
    void coldCacheNewUserRegistrationThenDifferentExistingUserFetchesJwksOnceAndExchangesCredentialOnlyForRegistration() throws Exception {
        prepareIdentityVerification("new-id-token", "new-apple-id");
        prepareIdentityVerification("other-id-token", "other-apple-id");
        User registeredUser = appleUser(3L, "new-apple-id", "new@example.com", Role.GUEST);
        User otherUser = appleUser(4L, "other-apple-id", "other@example.com", Role.USER);
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "new-apple-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, "other-apple-id"))
                .thenReturn(Optional.of(otherUser));
        when(userRepository.save(any(User.class))).thenReturn(registeredUser);
        when(authTokenService.issueLoginTokens(any(User.class), any())).thenReturn(
                new AuthTokenService.AuthTokens("access-token", "refresh-token")
        );
        JsonNode exchangeResponse = objectMapper.readTree("""
                {
                  "access_token": "apple-access",
                  "refresh_token": "apple-refresh",
                  "id_token": "identity-token",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
                """);
        when(appleRestTemplate.exchange(eq(APPLE_TOKEN_URL), eq(HttpMethod.POST), any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(exchangeResponse));

        appleLoginFilter.attemptAuthentication(
                appleRequest("new-id-token", "new-auth-code", "New User", "new@example.com"),
                new MockHttpServletResponse());
        appleLoginFilter.attemptAuthentication(
                appleRequest("other-id-token", "other-auth-code", "Other User", "other@example.com"),
                new MockHttpServletResponse());

        verify(appleRestTemplate, times(1)).getForObject(APPLE_KEYS_URL, ApplePublicKeyResponse.class);
        verify(appleRestTemplate, times(1)).exchange(eq(APPLE_TOKEN_URL), eq(HttpMethod.POST), any(), eq(JsonNode.class));
        verify(userAlarmSettingRepository).save(any(UserAlarmSetting.class));
        verify(analyticsPreferenceService).createDefaultPreference(registeredUser);
    }

    private void prepareIdentityVerification(String identityToken, String appleUserId) throws Exception {
        Map<String, String> headers = Map.of("kid", "key-id", "alg", "RS256");
        Claims claims = Jwts.claims()
                .setSubject(appleUserId)
                .setIssuer("https://appleid.apple.com")
                .setAudience("com.ontime.service")
                .setExpiration(Date.from(Instant.now().plusSeconds(60)));
        claims.put("email", appleUserId + "@example.com");

        when(jwtUtils.parseHeaders(identityToken)).thenReturn(headers);
        when(appleRestTemplate.getForObject(APPLE_KEYS_URL, ApplePublicKeyResponse.class))
                .thenReturn(appleKeys);
        when(applePublicKeyGenerator.generatePublicKey(headers, appleKeys)).thenReturn(applePublicKey);
        when(jwtUtils.getTokenClaims(identityToken, applePublicKey)).thenReturn(claims);
    }

    private void configureAppleClientSecretInputs() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        ECPrivateKey privateKey = (ECPrivateKey) generator.generateKeyPair().getPrivate();
        String encodedKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----";
        ReflectionTestUtils.setField(appleLoginService, "teamId", "TEAM123456");
        ReflectionTestUtils.setField(appleLoginService, "keyId", "KEY123456");
        ReflectionTestUtils.setField(
                appleLoginService,
                "privateKeyBase64",
                Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8))
        );
    }

    private MockHttpServletRequest appleRequest(String idToken, String authCode, String fullName, String email) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.setRequestURI("/oauth2/apple/login");
        request.setContent(objectMapper.writeValueAsBytes(Map.of(
                "idToken", idToken,
                "authCode", authCode,
                "fullName", fullName,
                "email", email
        )));
        return request;
    }

    private User appleUser(Long id, String appleUserId, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .name("Apple User")
                .spareTime(10)
                .note("note")
                .punctualityScore(95.0f)
                .role(role)
                .socialType(SocialType.APPLE)
                .socialId(appleUserId)
                .build();
    }
}
