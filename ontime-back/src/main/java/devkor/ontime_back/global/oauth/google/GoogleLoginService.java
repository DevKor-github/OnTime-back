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
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

@Slf4j
@Service
public class GoogleLoginService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/userinfo/v2/me";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke?token=";

    private final List<String> validClientIds;

    public GoogleLoginService(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            @Value("${google.web.client-id}") String webClientId,
            @Value("${google.app.client-id}") String appClientId
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.validClientIds = List.of(webClientId, appClientId);
    }


    public Authentication handleLogin(OAuthGoogleRequestDto oAuthGoogleRequestDto, User user, HttpServletResponse response) throws IOException {
        user.updateSocialLoginToken(oAuthGoogleRequestDto.getRefreshToken());

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        jwtTokenProvider.updateRefreshToken(user.getEmail(), refreshToken);
        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);

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

        String accessToken = jwtTokenProvider.createAccessToken(newUser.getEmail(), newUser.getId());
        jwtTokenProvider.sendAccessToken(response, accessToken);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser, null, Collections.singletonList(new SimpleGrantedAuthority(savedUser.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String msg = savedUser.getRole().name().equals("GUEST") ? "유저의 ROLE이 GUEST이므로 온보딩API를 호출해 온보딩을 진행해야합니다." : "로그인에 성공하였습니다.";
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
            log.info("유효하지 않은 idtoken 입니다.");
            return null;
        }
    }

    public boolean revokeToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        String googleRefreshToken = user.getSocialLoginToken();

        RestTemplate restTemplate = new RestTemplate();
        String revokeUrl = GOOGLE_REVOKE_URL + googleRefreshToken;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                revokeUrl, HttpMethod.POST, request, String.class);

        return response.getStatusCode().is2xxSuccessful();
    }

}

