package devkor.ontime_back.global.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.global.oauth.apple.AppleLoginFilter;
import devkor.ontime_back.global.oauth.apple.AppleLoginService;
import devkor.ontime_back.global.oauth.google.GoogleLoginFilter;
import devkor.ontime_back.global.oauth.google.GoogleLoginService;
import devkor.ontime_back.global.oauth.kakao.KakaoLoginFilter;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OAuthLoginFilterValidationTest {

    @Mock
    private GoogleLoginService googleLoginService;

    @Mock
    private AppleLoginService appleLoginService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAlarmSettingRepository userAlarmSettingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("구글 로그인 필터가 잘못된 요청을 400 validation 응답으로 처리한다")
    void googleLoginFilterRejectsInvalidRequest() throws Exception {
        GoogleLoginFilter filter = new GoogleLoginFilter(
                "/oauth2/google/login",
                objectMapper,
                validator,
                googleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/google/login", "{}"),
                response))
                .isInstanceOf(AuthenticationException.class);

        assertValidationResponse(response);
        verifyNoInteractions(googleLoginService, userRepository);
    }

    @Test
    @DisplayName("카카오 로그인 필터가 잘못된 요청을 400 validation 응답으로 처리한다")
    void kakaoLoginFilterRejectsInvalidRequest() throws Exception {
        KakaoLoginFilter filter = new KakaoLoginFilter(
                "/oauth2/kakao/login",
                objectMapper,
                validator,
                jwtTokenProvider,
                userRepository,
                userAlarmSettingRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/kakao/login", "{\"id\":\"\",\"profile\":{}}"),
                response))
                .isInstanceOf(AuthenticationException.class);

        assertValidationResponse(response);
        verifyNoInteractions(jwtTokenProvider, userRepository, userAlarmSettingRepository);
    }

    @Test
    @DisplayName("애플 로그인 필터가 잘못된 요청을 400 validation 응답으로 처리한다")
    void appleLoginFilterRejectsInvalidRequest() throws Exception {
        AppleLoginFilter filter = new AppleLoginFilter(
                "/oauth2/apple/login",
                objectMapper,
                validator,
                appleLoginService,
                userRepository);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.attemptAuthentication(
                request("/oauth2/apple/login", "{\"idToken\":\"\",\"authCode\":\"\",\"fullName\":\"\"}"),
                response))
                .isInstanceOf(AuthenticationException.class);

        assertValidationResponse(response);
        verifyNoInteractions(appleLoginService, userRepository);
    }

    private MockHttpServletRequest request(String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.setRequestURI(uri);
        request.setContent(body.getBytes());
        return request;
    }

    private void assertValidationResponse(MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("\"status\":\"error\"");
        assertThat(response.getContentAsString()).contains("\"code\":1002");
        assertThat(response.getContentAsString()).contains("\"errors\"");
    }
}
