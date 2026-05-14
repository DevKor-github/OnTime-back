package devkor.ontime_back.global.generallogin.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class LoginFailureHandlerTest {

    @Test
    void authenticationFailureReturnsPlainKoreanLoginFailureMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new LoginFailureHandler().onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad credentials")
        );

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("로그인 실패! 이메일이나 비밀번호를 확인해주세요.");
    }
}
