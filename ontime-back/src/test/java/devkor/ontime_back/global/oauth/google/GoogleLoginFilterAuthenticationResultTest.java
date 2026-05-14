package devkor.ontime_back.global.oauth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
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

class GoogleLoginFilterAuthenticationResultTest {

    private final TestableGoogleLoginFilter filter = new TestableGoogleLoginFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulAuthenticationStoresAuthenticationAndReturnsOk() throws Exception {
        User user = User.builder().id(1L).email("user@example.com").build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.callSuccessfulAuthentication(
                new MockHttpServletRequest(),
                response,
                mock(FilterChain.class),
                authentication
        );

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test
    void unsuccessfulAuthenticationWritesUnauthorizedEnvelopeForCredentialFailures() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.callUnsuccessfulAuthentication(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad token")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication failed");
    }

    private static class TestableGoogleLoginFilter extends GoogleLoginFilter {
        private TestableGoogleLoginFilter() {
            super(
                    "/oauth2/google/login",
                    new ObjectMapper(),
                    Validation.buildDefaultValidatorFactory().getValidator(),
                    mock(GoogleLoginService.class),
                    mock(UserRepository.class)
            );
        }

        public void callSuccessfulAuthentication(MockHttpServletRequest request,
                                                 MockHttpServletResponse response,
                                                 FilterChain chain,
                                                 org.springframework.security.core.Authentication authResult) throws java.io.IOException, jakarta.servlet.ServletException {
            super.successfulAuthentication(request, response, chain, authResult);
        }

        public void callUnsuccessfulAuthentication(MockHttpServletRequest request,
                                                   MockHttpServletResponse response,
                                                   org.springframework.security.core.AuthenticationException failed) throws java.io.IOException, jakarta.servlet.ServletException {
            super.unsuccessfulAuthentication(request, response, failed);
        }
    }
}
