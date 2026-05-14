package devkor.ontime_back.global.oauth.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.repository.UserRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AppleLoginFilterAuthenticationResultTest {

    private final TestableAppleLoginFilter filter = new TestableAppleLoginFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unsuccessfulAuthenticationWritesUnauthorizedEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.callUnsuccessfulAuthentication(new MockHttpServletRequest(), response, new BadCredentialsException("bad"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication failed");
    }

    private static class TestableAppleLoginFilter extends AppleLoginFilter {
        private TestableAppleLoginFilter() {
            super(
                    "/oauth2/apple/login",
                    new ObjectMapper(),
                    Validation.buildDefaultValidatorFactory().getValidator(),
                    mock(AppleLoginService.class),
                    mock(UserRepository.class)
            );
        }

        void callUnsuccessfulAuthentication(MockHttpServletRequest request,
                                            MockHttpServletResponse response,
                                            org.springframework.security.core.AuthenticationException failed) throws java.io.IOException, jakarta.servlet.ServletException {
            super.unsuccessfulAuthentication(request, response, failed);
        }
    }
}
