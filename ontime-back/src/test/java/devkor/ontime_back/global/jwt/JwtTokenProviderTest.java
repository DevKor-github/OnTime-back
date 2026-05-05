package devkor.ontime_back.global.jwt;

import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtTokenProviderTest {

    @DisplayName("동일한 시각에 발급한 리프레시 토큰도 서로 다르다")
    @Test
    void createRefreshTokenGeneratesUniqueTokens() {
        // given
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(mock(UserRepository.class));
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", "test-secret-key-that-is-long-enough");
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationPeriod", 3600000L);

        // when
        String refreshToken1 = jwtTokenProvider.createRefreshToken();
        String refreshToken2 = jwtTokenProvider.createRefreshToken();

        // then
        assertThat(refreshToken1).isNotEqualTo(refreshToken2);
    }
}
