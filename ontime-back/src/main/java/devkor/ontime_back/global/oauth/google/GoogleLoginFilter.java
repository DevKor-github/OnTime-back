package devkor.ontime_back.global.oauth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import devkor.ontime_back.dto.OAuthGoogleRequestDto;
import devkor.ontime_back.dto.OAuthGoogleUserDto;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GoogleLoginFilter extends AbstractAuthenticationProcessingFilter {
    private static final Map<String, Object> LOGIN_LOCKS = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final GoogleLoginService googleLoginService;


    public GoogleLoginFilter(String defaultFilterProcessesUrl, GoogleLoginService googleLoginService, UserRepository userRepository) {
        super(defaultFilterProcessesUrl);
        this.googleLoginService = googleLoginService;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        ObjectMapper objectMapper = new ObjectMapper();
        OAuthGoogleRequestDto oAuthGoogleRequestDto = objectMapper.readValue(request.getInputStream(), OAuthGoogleRequestDto.class);

        try {
            GoogleIdToken.Payload googlePayload = googleLoginService.verifyIdentityToken(oAuthGoogleRequestDto.getIdToken());
            String googleUserId = googlePayload.getSubject();

            Object loginLock = LOGIN_LOCKS.computeIfAbsent(googleUserId, key -> new Object());
            synchronized (loginLock) {
                List<User> existingUsers = userRepository.findAllBySocialTypeAndSocialIdOrderByIdDesc(SocialType.GOOGLE, googleUserId);

                if (!existingUsers.isEmpty()) {
                    if (existingUsers.size() > 1) {
                        log.warn("동일한 Google socialId를 가진 유저가 {}명 존재합니다. 최신 userId={} 계정으로 로그인합니다.",
                                existingUsers.size(), existingUsers.get(0).getId());
                    }
                    return googleLoginService.handleLogin(oAuthGoogleRequestDto, existingUsers.get(0), response);
                } else {
                    OAuthGoogleUserDto oAuthGoogleUserDto = new OAuthGoogleUserDto(googleUserId, (String) googlePayload.get("name"), (String) googlePayload.get("picture"), googlePayload.getEmail());
                    try {
                        return googleLoginService.handleRegister(oAuthGoogleRequestDto, oAuthGoogleUserDto, response);
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Google 회원가입 중 중복 socialId가 감지되어 기존 계정으로 로그인합니다. socialId={}", googleUserId);
                        User user = userRepository.findAllBySocialTypeAndSocialIdOrderByIdDesc(SocialType.GOOGLE, googleUserId)
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> e);
                        return googleLoginService.handleLogin(oAuthGoogleRequestDto, user, response);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Google 로그인 실패: {}", e.getMessage(), e);
            throw new AuthenticationException("Google 로그인 실패") {};
        }


    }

    // 인증 성공 처리
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {

        log.info("구글 로그인 성공");
        SecurityContextHolder.getContext().setAuthentication(authResult);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // 인증 실패 처리
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        log.warn("구글 로그인 실패");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"status\":\"error\", \"message\":\"Authentication failed\"}");
    }

}
