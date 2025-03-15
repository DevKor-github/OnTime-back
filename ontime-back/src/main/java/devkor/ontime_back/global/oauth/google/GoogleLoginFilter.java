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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class GoogleLoginFilter extends AbstractAuthenticationProcessingFilter {
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

            Optional<User> existingUser = userRepository.findBySocialTypeAndSocialId(SocialType.GOOGLE, googleUserId);

            if (existingUser.isPresent()) {
                return googleLoginService.handleLogin(oAuthGoogleRequestDto, existingUser.get(), response);
            } else {
                OAuthGoogleUserDto oAuthGoogleUserDto = new OAuthGoogleUserDto(googleUserId, (String) googlePayload.get("name"), (String) googlePayload.get("picture"), googlePayload.getEmail());
                return googleLoginService.handleRegister(oAuthGoogleRequestDto, oAuthGoogleUserDto, response);
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
