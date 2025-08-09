package devkor.ontime_back.global.generallogin.handler;

import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.persister.entity.EntityNameUse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${jwt.access.expiration}")
    private String accessTokenExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String email = extractUsername(authentication); // 인증 정보에서 Username(email) 추출
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    // 수정된 부분: User의 ID(PK)를 AccessToken 생성에 사용
                    String accessToken = jwtTokenProvider.createAccessToken(email, user.getId());

                    String refreshToken = jwtTokenProvider.createRefreshToken(); // RefreshToken 발급

                    // 수정된 부분: 응답 헤더에 AccessToken, RefreshToken 실어서 응답
                    jwtTokenProvider.sendAccessAndRefreshToken(response, accessToken, refreshToken);

                    user.updateAccessToken(accessToken);
                    user.updateRefreshToken(refreshToken);
                    userRepository.saveAndFlush(user);

                    log.info("로그인에 성공하였습니다. 이메일 : {}", email);
                    log.info("로그인에 성공하였습니다. AccessToken : {}", accessToken);
                    log.info("발급된 AccessToken 만료 기간 : {}", accessTokenExpiration);


                    try {
                        // 응답 Content-Type 설정
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

                        // 응답 바디에 작성
                        response.getWriter().write(responseBody);
                        response.getWriter().flush();
                    } catch (IOException e) {
                        log.error("응답 바디 작성 중 오류 발생", e);
                    }
                });
    }

    private String extractUsername(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }
}
