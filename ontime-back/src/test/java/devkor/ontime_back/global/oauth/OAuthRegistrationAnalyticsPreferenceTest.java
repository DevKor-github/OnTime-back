package devkor.ontime_back.global.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.OAuthAppleUserDto;
import devkor.ontime_back.dto.OAuthGoogleRequestDto;
import devkor.ontime_back.dto.OAuthGoogleUserDto;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.global.jwt.JwtUtils;
import devkor.ontime_back.global.oauth.apple.AppleLoginService;
import devkor.ontime_back.global.oauth.apple.ApplePublicKeyGenerator;
import devkor.ontime_back.global.oauth.google.GoogleLoginService;
import devkor.ontime_back.global.oauth.kakao.KakaoLoginFilter;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.AnalyticsPreferenceService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthRegistrationAnalyticsPreferenceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAlarmSettingRepository userAlarmSettingRepository;

    @Mock
    private AnalyticsPreferenceService analyticsPreferenceService;

    @Mock
    private ApplePublicKeyGenerator applePublicKeyGenerator;

    @Mock
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("구글 신규 가입은 계정 분석 설정 기본 행을 생성한다")
    void googleRegisterCreatesAnalyticsPreference() throws Exception {
        GoogleLoginService googleLoginService = new GoogleLoginService(
                jwtTokenProvider,
                userRepository,
                userAlarmSettingRepository,
                analyticsPreferenceService,
                "123-web.apps.googleusercontent.com",
                "123-app.apps.googleusercontent.com"
        );
        when(userRepository.save(any())).thenAnswer(invocation -> {
            Object user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        when(jwtTokenProvider.createAccessToken(anyString(), any())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");

        OAuthGoogleRequestDto requestDto = new OAuthGoogleRequestDto();
        ReflectionTestUtils.setField(requestDto, "refreshToken", "google-refresh-token");
        OAuthGoogleUserDto userDto = new OAuthGoogleUserDto(
                "google-id",
                "Google User",
                "https://example.com/profile.png",
                "user@example.com"
        );

        googleLoginService.handleRegister(requestDto, userDto, new MockHttpServletResponse());

        verify(analyticsPreferenceService).createDefaultPreference(any());
    }

    @Test
    @DisplayName("애플 신규 가입은 계정 분석 설정 기본 행을 생성한다")
    void appleRegisterCreatesAnalyticsPreference() throws Exception {
        AppleLoginService appleLoginService = new AppleLoginService(
                applePublicKeyGenerator,
                jwtUtils,
                userRepository,
                userAlarmSettingRepository,
                jwtTokenProvider,
                analyticsPreferenceService
        );
        when(userRepository.save(any())).thenAnswer(invocation -> {
            Object user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        when(jwtTokenProvider.createAccessToken(anyString(), any())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");

        OAuthAppleUserDto userDto = new OAuthAppleUserDto("apple-id", "user@example.com", "Apple User");

        appleLoginService.handleRegister("apple-refresh-token", userDto, new MockHttpServletResponse());

        verify(analyticsPreferenceService).createDefaultPreference(any());
    }

    @Test
    @DisplayName("카카오 신규 가입은 계정 분석 설정 기본 행을 생성한다")
    void kakaoRegisterCreatesAnalyticsPreference() throws Exception {
        KakaoLoginFilter filter = new KakaoLoginFilter(
                "/oauth2/kakao/login",
                objectMapper,
                validator,
                jwtTokenProvider,
                userRepository,
                userAlarmSettingRepository,
                analyticsPreferenceService
        );
        when(userRepository.findBySocialTypeAndSocialId(any(), anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> {
            Object user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        when(jwtTokenProvider.createAccessToken(isNull(), any())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken()).thenReturn("refresh-token");

        filter.attemptAuthentication(
                request("""
                        {
                          "id": "kakao-id",
                          "profile": {
                            "nickname": "Kakao User",
                            "thumbnailImageUrl": "https://example.com/thumb.png",
                            "profile_image_url": "https://example.com/profile.png",
                            "defaultImage": false,
                            "defaultNickname": false
                          }
                        }
                        """),
                new MockHttpServletResponse()
        );

        verify(analyticsPreferenceService).createDefaultPreference(any());
    }

    private MockHttpServletRequest request(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setMethod("POST");
        request.setRequestURI("/oauth2/kakao/login");
        request.setContent(body.getBytes());
        return request;
    }
}
