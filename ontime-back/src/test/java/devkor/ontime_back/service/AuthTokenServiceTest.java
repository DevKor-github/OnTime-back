package devkor.ontime_back.service;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserRefreshToken;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserRefreshTokenRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidAccessTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRefreshTokenRepository userRefreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(jwtTokenProvider, userRefreshTokenRepository, userRepository);
    }

    @Test
    void validateActiveAccessTokenReadsUserOnceAndReturnsTheAuthenticatedUser() {
        User user = user();
        user.updateAccessToken("access-token");
        when(jwtTokenProvider.extractUserId("access-token")).thenReturn(Optional.of(1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User authenticatedUser = authTokenService.validateActiveAccessToken("access-token");

        assertThat(authenticatedUser).isSameAs(user);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void validateActiveAccessTokenRejectsAReplacedAccessToken() {
        User user = user();
        user.updateAccessToken("newer-access-token");
        when(jwtTokenProvider.extractUserId("access-token")).thenReturn(Optional.of(1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authTokenService.validateActiveAccessToken("access-token"))
                .isInstanceOf(InvalidAccessTokenException.class);

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void issueLoginTokensClearsPreviousRefreshCredentialsBeforeSavingTheNewLogin() {
        User user = user();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.createAccessToken("user@example.com", 1L))
                .thenReturn("access-token-a", "access-token-b");
        when(jwtTokenProvider.createRefreshToken())
                .thenReturn("refresh-token-a", "refresh-token-b");

        authTokenService.issueLoginTokens(user, response);
        authTokenService.issueLoginTokens(user, response);

        ArgumentCaptor<UserRefreshToken> tokenCaptor = ArgumentCaptor.forClass(UserRefreshToken.class);
        verify(userRefreshTokenRepository, times(2)).deleteByUser(user);
        verify(userRefreshTokenRepository, times(2)).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getAllValues())
                .extracting(UserRefreshToken::getRefreshToken)
                .containsExactly("refresh-token-a", "refresh-token-b");
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token-b");
        verify(jwtTokenProvider).sendAccessAndRefreshToken(response, "access-token-a", "refresh-token-a");
        verify(jwtTokenProvider).sendAccessAndRefreshToken(response, "access-token-b", "refresh-token-b");
    }

    @Test
    void rotateRefreshTokenUpdatesOnlyTheMatchedSessionToken() {
        User user = user();
        UserRefreshToken storedToken = UserRefreshToken.create(user, "refresh-token-a");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userRefreshTokenRepository.findByRefreshToken("refresh-token-a"))
                .thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.createAccessToken("user@example.com", 1L))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken())
                .thenReturn("new-refresh-token-a");

        AuthTokenService.AuthTokens tokens = authTokenService.rotateRefreshToken("refresh-token-a", response);

        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh-token-a");
        assertThat(storedToken.getRefreshToken()).isEqualTo("new-refresh-token-a");
        assertThat(user.getAccessToken()).isEqualTo("new-access-token");
        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token-a");
        verify(jwtTokenProvider).sendAccessAndRefreshToken(response, "new-access-token", "new-refresh-token-a");
        verify(userRepository).saveAndFlush(user);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .role(Role.USER)
                .build();
    }
}
