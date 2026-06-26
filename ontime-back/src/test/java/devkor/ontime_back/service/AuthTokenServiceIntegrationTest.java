package devkor.ontime_back.service;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidAccessTokenException;
import devkor.ontime_back.response.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
class AuthTokenServiceIntegrationTest {

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void loggingInAgainMakesPreviousRefreshCredentialUnusable() {
        User user = userRepository.saveAndFlush(user());
        MockHttpServletResponse firstLoginResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondLoginResponse = new MockHttpServletResponse();

        AuthTokenService.AuthTokens firstLoginTokens = authTokenService.issueLoginTokens(user, firstLoginResponse);
        AuthTokenService.AuthTokens secondLoginTokens = authTokenService.issueLoginTokens(user, secondLoginResponse);

        assertThatCode(() -> authTokenService.rotateRefreshToken(firstLoginTokens.refreshToken(), new MockHttpServletResponse()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatCode(() -> authTokenService.rotateRefreshToken(secondLoginTokens.refreshToken(), new MockHttpServletResponse()))
                .doesNotThrowAnyException();
    }

    @Test
    void loggingInAgainMakesPreviousAccessCredentialUnusable() {
        User user = userRepository.saveAndFlush(user());
        AuthTokenService.AuthTokens firstLoginTokens =
                authTokenService.issueLoginTokens(user, new MockHttpServletResponse());

        authTokenService.issueLoginTokens(user, new MockHttpServletResponse());

        assertThatCode(() -> jwtTokenProvider.isAccessTokenValid(firstLoginTokens.accessToken()))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    private User user() {
        return User.builder()
                .email("single-session@example.com")
                .password("password")
                .name("single-session-user")
                .role(Role.USER)
                .build();
    }
}
