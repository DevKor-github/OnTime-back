package devkor.ontime_back.global.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidAccessTokenException;
import devkor.ontime_back.response.InvalidRefreshTokenException;
import devkor.ontime_back.response.InvalidTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Getter
@Slf4j
@Transactional
public class JwtTokenProvider {

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${jwt.access.expiration}")
    private Long accessTokenExpirationPeriod;

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenExpirationPeriod;

    @Value("${jwt.access.header}")
    private String accessHeader;

    @Value("${jwt.refresh.header}")
    private String refreshHeader;

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String EMAIL_CLAIM = "email"; // jwt claim으로 email 사용
    private static final String BEARER = "Bearer "; // 'Authorization(Key) = Bearer {토큰} (Value)' 형식
    private static final String USER_ID_CLAIM = "userId";

    private final UserRepository userRepository;

    // accessToken 생성
    public String createAccessToken(String email, Long userId) {
        Date now = new Date();
        return JWT.create()
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .withExpiresAt(new Date(now.getTime() + accessTokenExpirationPeriod))
                .withJWTId(UUID.randomUUID().toString())
                .withClaim(EMAIL_CLAIM, email)
                .withClaim(USER_ID_CLAIM, userId)
                .sign(Algorithm.HMAC512(secretKey));
    }

    // refreshToken 생성
    public String createRefreshToken() {
        Date now = new Date();
        return JWT.create()
                .withSubject(REFRESH_TOKEN_SUBJECT)
                .withExpiresAt(new Date(now.getTime() + refreshTokenExpirationPeriod))
                .withJWTId(UUID.randomUUID().toString())
                .sign(Algorithm.HMAC512(secretKey));
    }

    // accessToken header에 넣어서 전송
    public void sendAccessToken(HttpServletResponse response, String accessToken) {
        response.setStatus(HttpServletResponse.SC_OK);

        response.setHeader(accessHeader, accessToken);
    }

    // accessToken + refreshToken header에 넣어서 전송
    public void sendAccessAndRefreshToken(HttpServletResponse response, String accessToken, String refreshToken) {
        response.setStatus(HttpServletResponse.SC_OK);

        setAccessTokenHeader(response, accessToken);
        setRefreshTokenHeader(response, refreshToken);
        log.info("Credential headers set");
    }

    // header에서 refreshToken 추출
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(refreshHeader))
                .filter(refreshToken -> refreshToken.startsWith(BEARER))
                .map(refreshToken -> refreshToken.replace(BEARER, "")); // 'Bearer XXX' 형식에서 "Bearer"를 삭제
    }

    // header에서 accessToken 추출
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(accessHeader))
                .filter(refreshToken -> refreshToken.startsWith(BEARER))
                .map(refreshToken -> refreshToken.replace(BEARER, ""));
    }

    // accessToken에서 email 추출
    public Optional<String> extractEmail(String accessToken) {
        try {
            return Optional.ofNullable(JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(accessToken)
                    .getClaim(EMAIL_CLAIM)
                    .asString());
        } catch (Exception e) {
            log.error("Access credential is invalid");
            return Optional.empty();
        }
    }

    // accessToken에서 userId 추출
    public Optional<Long> extractUserId(String accessToken) {
        try {
            return Optional.ofNullable(JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(accessToken)
                    .getClaim(USER_ID_CLAIM)
                    .asLong());
        } catch (Exception e) {
            log.error("Access credential is invalid");
            return Optional.empty();
        }
    }

    // accessToken header 설정
    public void setAccessTokenHeader(HttpServletResponse response, String accessToken) {
        response.setHeader(accessHeader, accessToken);
    }

    // refreshToken header 설정
    public void setRefreshTokenHeader(HttpServletResponse response, String refreshToken) {
        response.setHeader(refreshHeader, refreshToken);
    }

    // token 유효성 확인
    public boolean isTokenValid(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secretKey)).build().verify(token);
            log.info("Credential is valid");
            return true;
        } catch (Exception e) {
            log.error("Credential is invalid");
            throw new InvalidTokenException("유효하지 않은 토큰입니다.");
        }
    }

    public boolean isAccessTokenValid(String token) {
        try {
            Long userId = JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token)
                    .getClaim(USER_ID_CLAIM)
                    .asLong();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new InvalidAccessTokenException("유효하지 않은 엑세스 토큰입니다."));
            if (!token.equals(user.getAccessToken())) {
                throw new InvalidAccessTokenException("유효하지 않은 엑세스 토큰입니다.");
            }
            log.info("Access credential is valid");
            return true;
        } catch (Exception e) {
            log.error("Access credential is invalid");
            throw new InvalidAccessTokenException("유효하지 않은 엑세스 토큰입니다.");
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secretKey)).build().verify(token);
            log.info("Refresh credential is valid");
            return true;
        } catch (Exception e) {
            log.error("Refresh credential is invalid");
            throw new InvalidRefreshTokenException("유효하지 않은 리프레시 토큰입니다.");
        }
    }

    public String createExpiredAccessToken(String email) {
        Date now = new Date();
        return JWT.create()
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .withExpiresAt(new Date(now.getTime() - 36000000)) // 현재 시간보다 1초 이전으로 설정
                .withClaim(EMAIL_CLAIM, email)
                .sign(Algorithm.HMAC512(secretKey));
    }

}
