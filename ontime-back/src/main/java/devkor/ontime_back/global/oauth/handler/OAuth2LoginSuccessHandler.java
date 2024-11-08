package devkor.ontime_back.global.oauth.handler;

import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.global.oauth.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");
        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            // User의 Role이 GUEST -> 회원가입 페이지로 리다이렉트
            if(oAuth2User.getRole() == Role.GUEST) {
                String accessToken = jwtTokenProvider.createAccessToken(oAuth2User.getEmail(), oAuth2User.getUserId());
                log.info("회원가입 accessToken 확인 {}", accessToken);
                response.addHeader(jwtTokenProvider.getAccessHeader(), "Bearer " + accessToken);
                response.sendRedirect("/oauth2/sign-up");
            } else {
                loginSuccess(response, oAuth2User); // 로그인에 성공한 경우 access, refresh 토큰 생성
            }
        } catch (Exception e) {
            throw e;
        }

    }
    private void loginSuccess(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {
        String accessToken = jwtTokenProvider.createAccessToken(oAuth2User.getEmail(), oAuth2User.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken();
        log.info("accessToken 확인 {}", accessToken);
        response.addHeader(jwtTokenProvider.getAccessHeader(), "Bearer " + accessToken);
        response.addHeader(jwtTokenProvider.getRefreshHeader(), "Bearer " + refreshToken);

        jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        jwtTokenProvider.updateRefreshToken(oAuth2User.getEmail(), refreshToken);
    }
}