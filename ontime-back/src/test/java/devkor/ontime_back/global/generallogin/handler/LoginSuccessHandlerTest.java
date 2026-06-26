package devkor.ontime_back.global.generallogin.handler;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Test
    void successfulUserLoginRotatesTokensAndWritesUserResponse() throws Exception {
        LoginSuccessHandler handler = new LoginSuccessHandler(jwtTokenProvider, userRepository);
        User user = user(Role.USER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken("user@example.com", 1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), authentication());

        assertThat(user.getAccessToken()).isEqualTo("access-token");
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");
        verify(jwtTokenProvider).sendAccessAndRefreshToken(any(), eq("access-token"), eq("refresh-token"));
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void successfulGuestLoginTellsClientToContinueOnboarding() throws Exception {
        LoginSuccessHandler handler = new LoginSuccessHandler(jwtTokenProvider, userRepository);
        User user = user(Role.GUEST);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken("user@example.com", 1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication());

        assertThat(response.getContentAsString()).contains("온보딩API를 호출해 온보딩을 진행해야합니다.");
        assertThat(response.getContentAsString()).contains("\"role\": \"GUEST\"");
    }

    @Test
    void successfulAuthenticationDoesNothingWhenEmailNoLongerExists() throws Exception {
        LoginSuccessHandler handler = new LoginSuccessHandler(jwtTokenProvider, userRepository);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), authentication());

        verifyNoInteractions(jwtTokenProvider);
        verify(userRepository, never()).saveAndFlush(any());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("user@example.com")
                .password("password")
                .roles("USER")
                .build();
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private User user(Role role) {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .name("User")
                .spareTime(10)
                .note("note")
                .punctualityScore(95.0f)
                .role(role)
                .build();
    }
}
