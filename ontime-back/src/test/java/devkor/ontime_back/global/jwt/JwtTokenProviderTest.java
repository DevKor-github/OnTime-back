package devkor.ontime_back.global.jwt;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidAccessTokenException;
import devkor.ontime_back.response.InvalidRefreshTokenException;
import devkor.ontime_back.response.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private UserRepository userRepository;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(userRepository);
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", "test-secret-key-that-is-long-enough");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationPeriod", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationPeriod", 7200000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessHeader", "Authorization");
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshHeader", "Authorization-Refresh");
    }

    @DisplayName("동일한 시각에 발급한 리프레시 토큰도 서로 다르다")
    @Test
    void createRefreshTokenGeneratesUniqueTokens() {
        String refreshToken1 = jwtTokenProvider.createRefreshToken();
        String refreshToken2 = jwtTokenProvider.createRefreshToken();

        assertThat(refreshToken1).isNotEqualTo(refreshToken2);
    }

    @Test
    void accessTokenCarriesEmailAndUserIdClaims() {
        String accessToken = jwtTokenProvider.createAccessToken("user@example.com", 7L);

        assertThat(jwtTokenProvider.extractEmail(accessToken)).contains("user@example.com");
        assertThat(jwtTokenProvider.extractUserId(accessToken)).contains(7L);
        assertThat(jwtTokenProvider.isTokenValid(accessToken)).isTrue();
    }

    @Test
    void bearerTokenExtractionRejectsHeadersWithoutBearerPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "raw-access-token");
        request.addHeader("Authorization-Refresh", "Bearer refresh-token");

        assertThat(jwtTokenProvider.extractAccessToken(request)).isEmpty();
        assertThat(jwtTokenProvider.extractRefreshToken(request)).contains("refresh-token");
    }

    @Test
    void sendAccessAndRefreshTokenWritesBothCredentialHeaders() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtTokenProvider.sendAccessAndRefreshToken(response, "access-token", "refresh-token");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Authorization")).isEqualTo("access-token");
        assertThat(response.getHeader("Authorization-Refresh")).isEqualTo("refresh-token");
    }

    @Test
    void sendAccessTokenWritesOnlyTheAccessCredentialHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtTokenProvider.sendAccessToken(response, "access-token");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Authorization")).isEqualTo("access-token");
        assertThat(response.getHeader("Authorization-Refresh")).isNull();
    }

    @Test
    void updateRefreshTokenMutatesExistingUserRefreshToken() {
        User user = user("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        jwtTokenProvider.updateRefreshToken("user@example.com", "new-refresh-token");

        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void updateRefreshTokenFailsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jwtTokenProvider.updateRefreshToken("missing@example.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일치하는 회원이 없습니다.");
    }

    @Test
    void accessTokenValidityRequiresTokenToBeStoredForAUser() {
        String accessToken = jwtTokenProvider.createAccessToken("user@example.com", 7L);
        when(userRepository.findByAccessToken(accessToken)).thenReturn(Optional.of(user("user@example.com")));

        assertThat(jwtTokenProvider.isAccessTokenValid(accessToken)).isTrue();
    }

    @Test
    void accessTokenValidityRejectsValidJwtThatIsNotStored() {
        String accessToken = jwtTokenProvider.createAccessToken("user@example.com", 7L);
        when(userRepository.findByAccessToken(accessToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jwtTokenProvider.isAccessTokenValid(accessToken))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void refreshTokenValidityRejectsExpiredTokensWithRefreshSpecificException() {
        String expiredAccessToken = jwtTokenProvider.createExpiredAccessToken("user@example.com");

        assertThatThrownBy(() -> jwtTokenProvider.isRefreshTokenValid(expiredAccessToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void genericTokenValidityRejectsMalformedCredentials() {
        assertThatThrownBy(() -> jwtTokenProvider.isTokenValid("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void claimExtractionReturnsEmptyForMalformedCredentials() {
        assertThat(jwtTokenProvider.extractEmail("not-a-jwt")).isEmpty();
        assertThat(jwtTokenProvider.extractUserId("not-a-jwt")).isEmpty();
    }

    @Test
    void refreshTokenValidityAcceptsValidRefreshCredential() {
        String refreshToken = jwtTokenProvider.createRefreshToken();

        assertThat(jwtTokenProvider.isRefreshTokenValid(refreshToken)).isTrue();
    }

    private User user(String email) {
        return User.builder()
                .id(7L)
                .email(email)
                .role(Role.USER)
                .build();
    }
}
