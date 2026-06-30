package devkor.ontime_back.global.jwt;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidAccessTokenException;
import devkor.ontime_back.response.InvalidRefreshTokenException;
import devkor.ontime_back.service.AuthTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("공개 HTML 페이지는 액세스 토큰 없이 JWT 필터를 통과한다.")
    @ParameterizedTest
    @ValueSource(strings = {"/account-deletion", "/account-deletion/en", "/privacy-policy", "/privacy-policy/en"})
    void skipsPublicHtmlPages(String path) throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).extractAccessToken(request);
        verify(jwtTokenProvider, never()).extractRefreshToken(request);
    }

    @Test
    void validAccessTokenAuthenticatesUserAndContinuesFilterChain() throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/schedules");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user("user@example.com", "encoded-password");

        when(jwtTokenProvider.extractAccessToken(request)).thenReturn(Optional.of("access-token"));
        when(jwtTokenProvider.extractRefreshToken(request)).thenReturn(Optional.empty());
        when(authTokenService.validateActiveAccessToken("access-token")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(authTokenService).validateActiveAccessToken("access-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@example.com");
    }

    @Test
    void validRefreshTokenReissuesAccessTokenWithoutContinuingRequest() throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/schedules");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user("user@example.com", "encoded-password");

        when(jwtTokenProvider.extractAccessToken(request)).thenReturn(Optional.empty());
        when(jwtTokenProvider.extractRefreshToken(request)).thenReturn(Optional.of("refresh-token"));
        when(jwtTokenProvider.isRefreshTokenValid("refresh-token")).thenReturn(true);
        when(authTokenService.rotateRefreshToken("refresh-token", response))
                .thenReturn(new AuthTokenService.AuthTokens("new-access-token", "new-refresh-token"));

        filter.doFilter(request, response, filterChain);

        verify(authTokenService).rotateRefreshToken("refresh-token", response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void missingAccessTokenReturnsTokenEmptyErrorEnvelope() throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/schedules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.extractAccessToken(request)).thenReturn(Optional.empty());
        when(jwtTokenProvider.extractRefreshToken(request)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":\"accessTokenEmpty\"");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void invalidRefreshTokenReturnsRefreshSpecificErrorEnvelope() throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/schedules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.extractAccessToken(request)).thenReturn(Optional.empty());
        when(jwtTokenProvider.extractRefreshToken(request)).thenReturn(Optional.of("refresh-token"));
        when(jwtTokenProvider.isRefreshTokenValid("refresh-token"))
                .thenThrow(new InvalidRefreshTokenException("bad refresh"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":\"refreshTokenInvalid\"");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void invalidAccessTokenReturnsAccessSpecificErrorEnvelope() throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuthTokenService authTokenService = mock(AuthTokenService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/schedules");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.extractAccessToken(request)).thenReturn(Optional.of("access-token"));
        when(jwtTokenProvider.extractRefreshToken(request)).thenReturn(Optional.empty());
        when(authTokenService.validateActiveAccessToken("access-token"))
                .thenThrow(new InvalidAccessTokenException("bad access"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":\"accessTokenInvalid\"");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void socialLoginUserWithoutPasswordReceivesGeneratedAuthenticationPassword() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(AuthTokenService.class));
        User user = user("social@example.com", null);

        filter.saveAuthentication(user);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("social@example.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void socialLoginUserWithoutEmailUsesUserIdAuthenticationName() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(AuthTokenService.class));
        User user = user(null, null);

        filter.saveAuthentication(user);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user:1");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    private User user(String email, String password) {
        return User.builder()
                .id(1L)
                .email(email)
                .password(password)
                .role(Role.USER)
                .build();
    }
}
