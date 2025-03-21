package devkor.ontime_back.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.*;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

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

    private static final List<String> NO_CHECK_URLS = List.of("/login", "/swagger-ui", "/sign-up", "/v3/api-docs", "/oauth2/google/login", "/oauth2/kakao/login", "/oauth2/apple/login");

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        try {
            if (NO_CHECK_URLS.stream().anyMatch(requestURI::startsWith)) {
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken= jwtTokenProvider.extractAccessToken(request)
                    .orElse(null);

            String refreshToken = jwtTokenProvider.extractRefreshToken(request)
                    .orElse(null);

            // 리프레시 토큰이 있고, 유효할 경우 엑세스토큰 재발급
            // 이때 조건절의 isRefreshTokenValid에서 토큰이 유효하지 않으면 InvalidRefreshTokenException 발생
            if (refreshToken != null && jwtTokenProvider.isRefreshTokenValid(refreshToken)) {
                // 리프레시토큰의 경우 토큰의 유효성 뿐만 아니라 DB에 등록되어 있는지도 확인해야 함
                // reIssueAccessToken 메소드에서 DB를 확인해 등록된 리프레시 토큰이면 엑세스 토큰 재발급
                // 이때 reIssueAccessToken 메소드에서 DB에 등록된 리프레시 토큰이 아니면 InvalidRefreshTokenException 발생
                log.info("리프레시 토큰이 있고 유효한데 DB에 있는지는 아직 모름");
                reIssueAccessToken(response, refreshToken);
                return;
            }

            // 리프레시 토큰이 있을 때의 처리는 위의 if문에서 처리하였음.
            // 이제부터는 엑세스 토큰'만' 헤더에 담긴 요청만 생각하면 됨

            // 엑세스 토큰이 있고, 유효할 경우 checkAccessTokenAndAuthentication 메서드 호출해 권한정보 저장하고 스프링 시큐리티 필터체인 계속 진행
            if (accessToken != null && jwtTokenProvider.isAccessTokenValid(accessToken)) {
                checkAccessTokenAndAuthentication(request, response, filterChain);
            }

            // 엑세스 토큰이 없는 경우 EmptyAccessTokenException 발생
            // 엑세스 토큰 없고 리프레시 토큰 있는 경우는 첫번째 if문에서 처리하여서 고려하지 않아도 됨.
            if (accessToken == null) {
                throw new EmptyAccessTokenException("Empty Access token!~!");
            }


        }
        catch (InvalidTokenException ex) {
            handleInvalidTokenException(response, ex);
        }

    }

    // 리프레시 토큰이 DB에 있으면 엑세스 토큰을 재발급
    // DB에 없으면 InvalidRefreshTokenException 발생
    public void reIssueAccessToken(HttpServletResponse response, String refreshToken) throws IOException {
        log.info("리프레시토큰이 유효하나 DB에 있는지는 모름. DB에서 찾아봐서 없으면 예외 발생할 것임.");
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid Refresh token!~!"));
        log.info("리프레시토큰이 DB에도 있음");
        jwtTokenProvider.sendAccessToken(response, jwtTokenProvider.createAccessToken(user.getEmail(), user.getId()));
    }

    // accessToken으로 유저의 권한정보만 저장하고 인증 허가(스프링 시큐리티 필터체인 中 인증체인 통과해 다음 체인으로 이동)
    public void checkAccessTokenAndAuthentication(HttpServletRequest request, HttpServletResponse response,
                                                  FilterChain filterChain) throws ServletException, IOException {
        log.info("checkAccessTokenAndAuthentication() 호출");
        jwtTokenProvider.extractAccessToken(request)
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

        log.info("InvalidTokenException 발생");

        // ErrorCode에서 정보를 가져옴
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        if (ex instanceof InvalidRefreshTokenException) {
            ApiResponseForm<Void> errorResponse = ApiResponseForm.refreshTokenInvalid(
                    errorCode.getCode(),
                    errorCode.getMessage()
            );

            // ObjectMapper를 사용하여 JSON 변환 후 응답에 기록
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } else if (ex instanceof InvalidAccessTokenException) {
            ApiResponseForm<Void> errorResponse = ApiResponseForm.accessTokenInvalid(
                    errorCode.getCode(),
                    errorCode.getMessage()
            );

            // ObjectMapper를 사용하여 JSON 변환 후 응답에 기록
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } else if (ex instanceof EmptyAccessTokenException) {
            ApiResponseForm<Void> errorResponse = ApiResponseForm.accessTokenEmpty(
                    errorCode.getCode(),
                    errorCode.getMessage()
            );

            // ObjectMapper를 사용하여 JSON 변환 후 응답에 기록
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }
}