package devkor.ontime_back.global.oauth.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.AnalyticsPreferenceService;
import devkor.ontime_back.service.AuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KakaoLoginFilterAuthenticationResultTest {

    private final TestableKakaoLoginFilter filter = new TestableKakaoLoginFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulAuthenticationStoresAuthenticationAndReturnsOk() throws Exception {
        User user = User.builder().id(1L).email("kakao@example.com").build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.callSuccessfulAuthentication(new MockHttpServletRequest(), response, mock(FilterChain.class), authentication);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test
    void unsuccessfulAuthenticationWritesUnauthorizedEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.callUnsuccessfulAuthentication(new MockHttpServletRequest(), response, new BadCredentialsException("bad"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication failed");
    }

    private static class TestableKakaoLoginFilter extends KakaoLoginFilter {
        private TestableKakaoLoginFilter() {
            super(
                    "/oauth2/kakao/login",
                    new ObjectMapper(),
                    Validation.buildDefaultValidatorFactory().getValidator(),
                    mock(JwtTokenProvider.class),
                    mock(UserRepository.class),
                    mock(UserAlarmSettingRepository.class),
                    mock(AnalyticsPreferenceService.class),
                    mock(AuthTokenService.class)
            );
        }

        void callSuccessfulAuthentication(MockHttpServletRequest request,
                                          MockHttpServletResponse response,
                                          FilterChain chain,
                                          org.springframework.security.core.Authentication authResult) throws java.io.IOException, jakarta.servlet.ServletException {
            super.successfulAuthentication(request, response, chain, authResult);
        }

        void callUnsuccessfulAuthentication(MockHttpServletRequest request,
                                            MockHttpServletResponse response,
                                            org.springframework.security.core.AuthenticationException failed) throws java.io.IOException, jakarta.servlet.ServletException {
            super.unsuccessfulAuthentication(request, response, failed);
        }
    }
}
