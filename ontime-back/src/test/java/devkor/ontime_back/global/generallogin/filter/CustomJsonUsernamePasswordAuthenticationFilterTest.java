package devkor.ontime_back.global.generallogin.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomJsonUsernamePasswordAuthenticationFilterTest {

    @Mock
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("필터가 올바른 인증 요청을 처리한다")
    void testFilterProcessesCorrectRequest() throws Exception {
        // given
        String email = "user@example.com";
        String password = "password123!";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.setRequestURI("/login");
        request.setContent(objectMapper.writeValueAsBytes(Map.of("email", email, "password", password)));

        MockHttpServletResponse response = new MockHttpServletResponse();

        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(email, password);

        when(authenticationManager.authenticate(authRequest))
                .thenReturn(new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        CustomJsonUsernamePasswordAuthenticationFilter filter = new CustomJsonUsernamePasswordAuthenticationFilter(objectMapper, validator);
        filter.setAuthenticationManager(authenticationManager);

        // when
        Authentication result = filter.attemptAuthentication(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isEqualTo(email);
    }

    @Test
    @DisplayName("필터가 잘못된 로그인 요청을 400 validation 응답으로 처리한다")
    void invalidLoginRequestReturnsValidationError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.setRequestURI("/login");
        request.setContent(objectMapper.writeValueAsBytes(Map.of("email", "not-email", "password", "")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        CustomJsonUsernamePasswordAuthenticationFilter filter = new CustomJsonUsernamePasswordAuthenticationFilter(objectMapper, validator);
        filter.setAuthenticationManager(authenticationManager);

        assertThatThrownBy(() -> filter.attemptAuthentication(request, response))
                .isInstanceOf(AuthenticationException.class);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("\"status\":\"error\"");
        assertThat(response.getContentAsString()).contains("\"code\":1002");
        assertThat(response.getContentAsString()).contains("\"errors\"");
    }
}
