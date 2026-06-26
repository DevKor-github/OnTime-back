package devkor.ontime_back.global.oauth.google;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import devkor.ontime_back.dto.OAuthGoogleRequestDto;
import devkor.ontime_back.dto.OAuthGoogleUserDto;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.AnalyticsPreferenceService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class GoogleLoginService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserAlarmSettingRepository userAlarmSettingRepository;
    private final AnalyticsPreferenceService analyticsPreferenceService;
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/userinfo/v2/me";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke?token=";

    private final List<String> validClientIds;
    private final RestTemplate revokeRestTemplate;

    @Autowired
    public GoogleLoginService(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            UserAlarmSettingRepository userAlarmSettingRepository,
            AnalyticsPreferenceService analyticsPreferenceService,
            @Value("${google.web.client-id}") String webClientId,
            @Value("${google.app.client-id}") String appClientId
    ) {
        this(jwtTokenProvider, userRepository, userAlarmSettingRepository, analyticsPreferenceService,
                webClientId, appClientId, createRevokeRestTemplate());
    }

    GoogleLoginService(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            UserAlarmSettingRepository userAlarmSettingRepository,
            AnalyticsPreferenceService analyticsPreferenceService,
            String webClientId,
            String appClientId,
            RestTemplate revokeRestTemplate
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userAlarmSettingRepository = userAlarmSettingRepository;
        this.analyticsPreferenceService = analyticsPreferenceService;
        this.revokeRestTemplate = revokeRestTemplate;
        this.validClientIds = Stream.concat(
                        Stream.of(webClientId),
                        Stream.of(appClientId.split(","))
                )
                .map(String::trim)
                .filter(clientId -> !clientId.isBlank())
                .toList();
        log.info("Configured Google OAuth audiences: {}", validClientIds.stream()
                .map(this::maskClientId)
                .toList());
    }

    private static RestTemplate createRevokeRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);
        return new RestTemplate(requestFactory);
    }


    public Authentication handleLogin(OAuthGoogleRequestDto oAuthGoogleRequestDto, User user, HttpServletResponse response) throws IOException {
        user.updateSocialLoginToken(oAuthGoogleRequestDto.getRefreshToken());

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        user.updateAccessToken(accessToken);
        user.updateRefreshToken(refreshToken);
        userRepository.saveAndFlush(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String msg = user.getRole().name().equals("GUEST") ? "유저의 ROLE이 GUEST이므로 온보딩API를 호출해 온보딩을 진행해야합니다." : "로그인에 성공하였습니다.";
        // JSON 응답 생성
        String responseBody = String.format(
                "{ \"status\": \"success\", \"code\": \"200\", \"message\": \"%s\", \"data\": { " +
                        "\"userId\": %d, \"email\": \"%s\", \"name\": \"%s\", " +
                        "\"spareTime\": %d, \"note\": %s, \"punctualityScore\": %f, \"role\": \"%s\" } }",
                msg, user.getId(), user.getEmail(), user.getName(),
                user.getSpareTime(), user.getNote() != null ? "\"" + user.getNote() + "\"" : null, user.getPunctualityScore(), user.getRole().name()
        );

        response.getWriter().write(responseBody);
        response.getWriter().flush();

        return authentication;
    }

    public Authentication handleRegister(OAuthGoogleRequestDto oAuthGoogleRequestDto, OAuthGoogleUserDto oAuthGoogleUserDto, HttpServletResponse response) throws IOException {
        User newUser = User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId(oAuthGoogleUserDto.getId())
                .email(oAuthGoogleUserDto.getEmail())
                .name(oAuthGoogleUserDto.getName())
                .imageUrl(oAuthGoogleUserDto.getPicture())
                .role(Role.GUEST)
                .socialLoginToken(oAuthGoogleRequestDto.getRefreshToken())
                .build();

        UUID userSettingId = UUID.randomUUID();

        UserSetting userSetting = UserSetting.builder()
                .userSettingId(userSettingId)
                .user(newUser)
                .build();

        newUser.setUserSetting(userSetting);

        User savedUser = userRepository.save(newUser);

        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getEmail(), savedUser.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);

        savedUser.updateAccessToken(accessToken);
        savedUser.updateRefreshToken(refreshToken);
        userRepository.save(savedUser);
        userAlarmSettingRepository.save(UserAlarmSetting.defaultFor(savedUser));
        analyticsPreferenceService.createDefaultPreference(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser, null, Collections.singletonList(new SimpleGrantedAuthority(savedUser.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String msg = "회원가입에 성공하였습니다.";
        // JSON 응답 생성
        String responseBody = String.format(
                "{ \"status\": \"success\", \"code\": \"200\", \"message\": \"%s\", \"data\": { " +
                        "\"userId\": %d, \"email\": \"%s\", \"name\": \"%s\", " +
                        "\"spareTime\": %d, \"note\": %s, \"punctualityScore\": %f, \"role\": \"%s\" } }",
                msg, savedUser.getId(), savedUser.getEmail(), savedUser.getName(),
                savedUser.getSpareTime(), savedUser.getNote() != null ? "\"" + savedUser.getNote() + "\"" : null, savedUser.getPunctualityScore(), savedUser.getRole().name()
        );

        response.getWriter().write(responseBody);
        response.getWriter().flush();

        return authentication;
    }

    public GoogleIdToken.Payload verifyIdentityToken(String identityToken) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(validClientIds) // aud 확인
                .build();

        GoogleIdToken idToken = verifier.verify(identityToken); // Google의 공개 키를 사용하여 idToken 서명을 검증
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            return payload;
        } else {
            log.info("Google identity credential is invalid");
            logGoogleIdentityTokenClaims(identityToken);
            return null;
        }
    }

    private void logGoogleIdentityTokenClaims(String identityToken) {
        try {
            GoogleIdToken parsedToken = GoogleIdToken.parse(
                    GsonFactory.getDefaultInstance(),
                    identityToken
            );
            GoogleIdToken.Payload payload = parsedToken.getPayload();
            String audience = String.valueOf(payload.getAudience());
            Long expirationTimeSeconds = payload.getExpirationTimeSeconds();
            long nowSeconds = System.currentTimeMillis() / 1000;
            log.info(
                    "Google identity credential claims aud={}, azp={}, iss={}, exp={}, now={}, secondsUntilExp={}, audienceAllowed={}",
                    maskClientId(audience),
                    payload.get("azp"),
                    payload.getIssuer(),
                    expirationTimeSeconds,
                    nowSeconds,
                    expirationTimeSeconds == null ? null : expirationTimeSeconds - nowSeconds,
                    validClientIds.contains(audience)
            );
        } catch (Exception e) {
            log.info("Google identity credential claim parsing failed: {}", e.getClass().getSimpleName());
        }
    }

    private String maskClientId(String clientId) {
        int separatorIndex = clientId.indexOf('-');
        if (separatorIndex < 0) {
            return "<invalid-client-id>";
        }
        String projectNumber = clientId.substring(0, separatorIndex);
        String suffix = ".apps.googleusercontent.com";
        boolean hasGoogleSuffix = clientId.endsWith(suffix);
        String middle = clientId.substring(
                separatorIndex + 1,
                hasGoogleSuffix ? clientId.length() - suffix.length() : clientId.length()
        );
        String visibleTail = middle.length() <= 4 ? middle : middle.substring(middle.length() - 4);
        return projectNumber + "-..." + visibleTail + (hasGoogleSuffix ? suffix : "");
    }

    public boolean revokeToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        String googleRefreshToken = user.getSocialLoginToken();

        String revokeUrl = GOOGLE_REVOKE_URL + googleRefreshToken;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = revokeRestTemplate.exchange(
                revokeUrl, HttpMethod.POST, request, String.class);

        return response.getStatusCode().is2xxSuccessful();
    }

}
