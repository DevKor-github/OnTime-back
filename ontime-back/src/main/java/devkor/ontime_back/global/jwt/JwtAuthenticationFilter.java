package devkor.ontime_back.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.InvalidTokenException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.security.SecureRandom;

import jakarta.servlet.ServletException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;


// "/login" 이외의 URI 요청이 왔을 때 처리하는 필터
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String NO_CHECK_URL = "/login"; // "/login"으로 들어오는 요청은 Filter 작동 X

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (request.getRequestURI().equals(NO_CHECK_URL)) {
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken= jwtTokenProvider.extractAccessToken(request)
                    .orElse(null);

            String refreshToken = jwtTokenProvider.extractRefreshToken(request)
                    .orElse(null);

            if (accessToken != null && jwtTokenProvider.isTokenValid(accessToken)) {
                checkAccessTokenAndAuthentication(request, response, filterChain);
                return;
            }

            if (refreshToken != null) {
                checkRefreshTokenAndReIssueAccessToken(response, refreshToken);
                return;
            }

            if (accessToken != null && !jwtTokenProvider.isTokenValid(accessToken)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Access token.");
                return;
            }

        }
        catch (InvalidTokenException ex) {
            handleInvalidTokenException(response, ex);
        }

    }

    // refreshToken로 검색 후 accessToken 재발급 후 전송
    public void checkRefreshTokenAndReIssueAccessToken(HttpServletResponse response, String refreshToken) throws IOException {
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Refresh token.");
        }

        userRepository.findByRefreshToken(refreshToken) // refreshToken으로 유저 찾기
                .ifPresent(user -> {
                    String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId()); // accessToken 생성
                    log.info("New accessToken issued: " + newAccessToken); // 재발급된 accessToken 출력
                    jwtTokenProvider.sendAccessToken(response, newAccessToken); // accessToken 전송
                    // jwtTokenProvider.sendAccessToken(response, jwtTokenProvider.createAccessToken(user.getEmail(), user.getId())); // accessToken 생성 후 전송
                });
    }

    // accessToken 확인 후 인증 확인
    public void checkAccessTokenAndAuthentication(HttpServletRequest request, HttpServletResponse response,
                                                  FilterChain filterChain) throws ServletException, IOException {
        log.info("checkAccessTokenAndAuthentication() 호출");
        jwtTokenProvider.extractAccessToken(request)
                .filter(jwtTokenProvider::isTokenValid)
                .ifPresent(accessToken -> {
                    jwtTokenProvider.extractEmail(accessToken)
                            .ifPresent(email -> userRepository.findByEmail(email)
                                    .ifPresent(this::saveAuthentication)
                            );

                    jwtTokenProvider.extractUserId(accessToken)
                            .ifPresent(userId -> log.info("추출된 userId: {}", userId));
                });

        filterChain.doFilter(request, response);
    }

    // 인증 허가
    public void saveAuthentication(User myUser) {
        String password = myUser.getPassword();
        if (password == null) {
            password = generateRandomPassword(12); // 소셜 로그인 유저의 비밀번호 임의로 설정
        }

        UserDetails userDetailsUser = org.springframework.security.core.userdetails.User.builder()
                .username(myUser.getEmail())
                .password(password)
                .roles(myUser.getRole().name())
                .build();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetailsUser, null,
                        authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 비밀번호 랜덤 생성
    private String generateRandomPassword(int length) {
        final String CHAR_SET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()-_=+[]{}|;:,.<>?";
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder password = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(CHAR_SET.length());
            password.append(CHAR_SET.charAt(index));
        }

        return password.toString();
    }

    private void handleInvalidTokenException(HttpServletResponse response, InvalidTokenException ex) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // ErrorCode에서 정보를 가져옴
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        // ErrorCode를 사용하여 ApiResponseForm 생성
        ApiResponseForm<Void> errorResponse = ApiResponseForm.error(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        // ObjectMapper를 사용하여 JSON 변환 후 응답에 기록
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}