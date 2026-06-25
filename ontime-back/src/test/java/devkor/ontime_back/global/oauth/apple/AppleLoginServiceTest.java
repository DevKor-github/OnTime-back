package devkor.ontime_back.global.oauth.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.OAuthAppleUserDto;
import devkor.ontime_back.dto.AppleTokenResponseDto;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.global.jwt.JwtUtils;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleLoginServiceTest {

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

    private AppleLoginService appleLoginService;

    @BeforeEach
    void setUp() {
        appleLoginService = new AppleLoginService(
                applePublicKeyGenerator,
                jwtUtils,
                userRepository,
                userAlarmSettingRepository,
                jwtTokenProvider
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handleLoginRotatesAppleRefreshTokenAndApplicationTokens() throws Exception {
        User user = user(1L, "user@example.com", "Existing User", Role.USER);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.createAccessToken("user@example.com", 1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");

        Authentication authentication = appleLoginService.handleLogin("apple-refresh-token", user, response);

        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(user.getSocialLoginToken()).isEqualTo("apple-refresh-token");
        assertThat(user.getAccessToken()).isEqualTo("access-token");
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getContentAsString()).contains("\"message\": \"로그인에 성공하였습니다.\"");
        verify(jwtTokenProvider).sendAccessAndRefreshToken(response, "access-token", "refresh-token");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void handleRegisterCreatesGuestAppleUserAndDefaultAlarmSettings() throws Exception {
        OAuthAppleUserDto appleUser = new OAuthAppleUserDto("apple-id", "new@example.com", "New User");
        User savedUser = user(2L, "new@example.com", "New User", Role.GUEST);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.createAccessToken("new@example.com", 2L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication authentication = appleLoginService.handleRegister("apple-refresh-token", appleUser, response);

        assertThat(authentication.getPrincipal()).isSameAs(savedUser);
        assertThat(savedUser.getAccessToken()).isEqualTo("access-token");
        assertThat(savedUser.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getContentAsString()).contains("회원가입에 성공하였습니다.");
        verify(jwtTokenProvider).createAccessToken("new@example.com", 2L);
        verify(userRepository, times(2)).save(any(User.class));
        verify(userAlarmSettingRepository).save(any(UserAlarmSetting.class));
    }

    @Test
    void verifyIdentityTokenReturnsClaimsWhenIssuerAudienceAndExpirationAreValid() throws Exception {
        RestTemplate restTemplate = mockRestTemplate();
        ApplePublicKeyResponse appleKeys = new ApplePublicKeyResponse(java.util.List.of());
        PublicKey publicKey = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        Claims claims = Jwts.claims()
                .setIssuer("https://appleid.apple.com")
                .setAudience("com.ontime.service")
                .setExpiration(Date.from(Instant.now().plusSeconds(60)));
        ReflectionTestUtils.setField(appleLoginService, "clientId", "com.ontime.service");
        when(jwtUtils.parseHeaders("identity-token")).thenReturn(Map.of("kid", "key-id", "alg", "RS256"));
        when(restTemplate.getForObject("https://appleid.apple.com/auth/keys", ApplePublicKeyResponse.class))
                .thenReturn(appleKeys);
        when(applePublicKeyGenerator.generatePublicKey(Map.of("kid", "key-id", "alg", "RS256"), appleKeys))
                .thenReturn(publicKey);
        when(jwtUtils.getTokenClaims("identity-token", publicKey)).thenReturn(claims);

        Claims verifiedClaims = appleLoginService.verifyIdentityToken("identity-token");

        assertThat(verifiedClaims).isSameAs(claims);
    }

    @Test
    void verifyIdentityTokenRejectsTokenFromUnexpectedIssuer() throws Exception {
        RestTemplate restTemplate = mockRestTemplate();
        PublicKey publicKey = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        Claims claims = validAppleClaims().setIssuer("https://attacker.example");
        ReflectionTestUtils.setField(appleLoginService, "clientId", "com.ontime.service");
        when(jwtUtils.parseHeaders("identity-token")).thenReturn(Map.of());
        when(restTemplate.getForObject("https://appleid.apple.com/auth/keys", ApplePublicKeyResponse.class))
                .thenReturn(new ApplePublicKeyResponse(java.util.List.of()));
        when(applePublicKeyGenerator.generatePublicKey(any(), any())).thenReturn(publicKey);
        when(jwtUtils.getTokenClaims("identity-token", publicKey)).thenReturn(claims);

        assertThatThrownBy(() -> appleLoginService.verifyIdentityToken("identity-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void verifyIdentityTokenRejectsTokenForUnexpectedAudience() throws Exception {
        RestTemplate restTemplate = mockRestTemplate();
        PublicKey publicKey = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        Claims claims = validAppleClaims().setAudience("other-client-id");
        ReflectionTestUtils.setField(appleLoginService, "clientId", "com.ontime.service");
        when(jwtUtils.parseHeaders("identity-token")).thenReturn(Map.of());
        when(restTemplate.getForObject("https://appleid.apple.com/auth/keys", ApplePublicKeyResponse.class))
                .thenReturn(new ApplePublicKeyResponse(java.util.List.of()));
        when(applePublicKeyGenerator.generatePublicKey(any(), any())).thenReturn(publicKey);
        when(jwtUtils.getTokenClaims("identity-token", publicKey)).thenReturn(claims);

        assertThatThrownBy(() -> appleLoginService.verifyIdentityToken("identity-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("audience");
    }

    @Test
    void verifyIdentityTokenRejectsExpiredToken() throws Exception {
        RestTemplate restTemplate = mockRestTemplate();
        PublicKey publicKey = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
        Claims claims = validAppleClaims().setExpiration(Date.from(Instant.now().minusSeconds(1)));
        ReflectionTestUtils.setField(appleLoginService, "clientId", "com.ontime.service");
        when(jwtUtils.parseHeaders("identity-token")).thenReturn(Map.of());
        when(restTemplate.getForObject("https://appleid.apple.com/auth/keys", ApplePublicKeyResponse.class))
                .thenReturn(new ApplePublicKeyResponse(java.util.List.of()));
        when(applePublicKeyGenerator.generatePublicKey(any(), any())).thenReturn(publicKey);
        when(jwtUtils.getTokenClaims("identity-token", publicKey)).thenReturn(claims);

        assertThatThrownBy(() -> appleLoginService.verifyIdentityToken("identity-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("만료");
    }

    @Test
    void getAppleAccessTokenAndRefreshTokenExchangesAuthCodeWithClientSecret() throws Exception {
        RestTemplate restTemplate = mockRestTemplate();
        configureAppleClientSecretInputs();
        JsonNode responseBody = new ObjectMapper().readTree("""
                {
                  "access_token": "apple-access",
                  "refresh_token": "apple-refresh",
                  "id_token": "identity-token",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
                """);
        when(restTemplate.exchange(
                eq("https://appleid.apple.com/auth/token"),
                eq(HttpMethod.POST),
                any(),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        AppleTokenResponseDto response = appleLoginService.getAppleAccessTokenAndRefreshToken("auth-code");

        assertThat(response.getAccessToken()).isEqualTo("apple-access");
        assertThat(response.getRefreshToken()).isEqualTo("apple-refresh");
        assertThat(response.getIdToken()).isEqualTo("identity-token");
    }

    @Test
    void nativeAppleTokenRequestDoesNotIncludeRedirectUri() {
        ReflectionTestUtils.setField(appleLoginService, "clientId", "club.devkor.ontime.ios");

        MultiValueMap<String, String> body = appleLoginService.buildAppleTokenRequestBody(
                "apple-auth-code",
                "apple-client-secret");

        assertThat(body).containsEntry("grant_type", java.util.List.of("authorization_code"));
        assertThat(body).containsEntry("code", java.util.List.of("apple-auth-code"));
        assertThat(body).containsEntry("client_id", java.util.List.of("club.devkor.ontime.ios"));
        assertThat(body).containsEntry("client_secret", java.util.List.of("apple-client-secret"));
        assertThat(body).doesNotContainKey("redirect_uri");
    }

    @Test
    void revokeTokenReturnsFalseWhenAppleAcceptsCurrentRefreshToken() throws Exception {
        RestTemplate restTemplate = mockRestTemplate();
        configureAppleClientSecretInputs();
        User user = user(7L, "apple@example.com", "Apple User", Role.USER);
        user.updateSocialLoginToken("apple-refresh-token");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(restTemplate.exchange(
                eq("https://appleid.apple.com/auth/revoke"),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(""));

        boolean revoked = appleLoginService.revokeToken(7L);

        assertThat(revoked).isFalse();
    }

    @Test
    void revokeTokenReturnsTrueWhenAppleRejectsTheRefreshToken() throws Exception {
        RestTemplate restTemplate = mockRestTemplate();
        configureAppleClientSecretInputs();
        User user = user(7L, "apple@example.com", "Apple User", Role.USER);
        user.updateSocialLoginToken("apple-refresh-token");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(restTemplate.exchange(
                eq("https://appleid.apple.com/auth/revoke"),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        boolean revoked = appleLoginService.revokeToken(7L);

        assertThat(revoked).isTrue();
    }

    @Test
    void revokeTokenFailsWhenUserDoesNotExist() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appleLoginService.revokeToken(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found with id: 404");
    }

    private RestTemplate mockRestTemplate() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        ReflectionTestUtils.setField(appleLoginService, "restTemplate", restTemplate);
        return restTemplate;
    }

    private Claims validAppleClaims() {
        return Jwts.claims()
                .setIssuer("https://appleid.apple.com")
                .setAudience("com.ontime.service")
                .setExpiration(Date.from(Instant.now().plusSeconds(60)));
    }

    private void configureAppleClientSecretInputs() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        ECPrivateKey privateKey = (ECPrivateKey) generator.generateKeyPair().getPrivate();
        String encodedKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----";
        ReflectionTestUtils.setField(appleLoginService, "clientId", "com.ontime.service");
        ReflectionTestUtils.setField(appleLoginService, "teamId", "TEAM123456");
        ReflectionTestUtils.setField(appleLoginService, "keyId", "KEY123456");
        ReflectionTestUtils.setField(
                appleLoginService,
                "privateKeyBase64",
                Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8))
        );
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
                .socialType(SocialType.APPLE)
                .socialId("apple-id")
                .build();
    }
}
