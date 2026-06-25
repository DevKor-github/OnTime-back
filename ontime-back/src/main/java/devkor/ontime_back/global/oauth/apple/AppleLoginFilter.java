package devkor.ontime_back.global.oauth.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.AppleTokenResponseDto;
import devkor.ontime_back.dto.OAuthAppleRequestDto;
import devkor.ontime_back.dto.OAuthAppleUserDto;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.global.jwt.JwtUtils;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ValidationErrorWriter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class AppleLoginFilter extends AbstractAuthenticationProcessingFilter {

    private final AppleLoginService appleLoginService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    private final RestTemplate restTemplate = new RestTemplate();
    public AppleLoginFilter(String defaultFilterProcessesUrl, ObjectMapper objectMapper, Validator validator, AppleLoginService appleLoginService, UserRepository userRepository) {
        super(defaultFilterProcessesUrl);
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.appleLoginService = appleLoginService;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        OAuthAppleRequestDto oAuthAppleRequestDto = objectMapper.readValue(request.getInputStream(), OAuthAppleRequestDto.class);
        Set<ConstraintViolation<OAuthAppleRequestDto>> violations = validator.validate(oAuthAppleRequestDto);
        if (!violations.isEmpty()) {
            ValidationErrorWriter.write(response, objectMapper, violations);
            throw new RequestValidationException();
        }

        try {
            // Apple Identity Token 검증
            Claims tokenClaims = appleLoginService.verifyIdentityToken(oAuthAppleRequestDto.getIdToken());
            if (tokenClaims.getSubject() == null) {
                throw new IllegalStateException("Apple 로그인 검증 실패");
            }

            String appleUserId = tokenClaims.getSubject();
            String email = firstNonBlank(
                    oAuthAppleRequestDto.getEmail(),
                    tokenClaims.get("email", String.class)
            );
            String appleRefreshToken = exchangeAppleRefreshToken(oAuthAppleRequestDto);

            OAuthAppleUserDto oAuthAppleUserDto = new OAuthAppleUserDto(appleUserId, email, oAuthAppleRequestDto.getFullName());

            Optional<User> existingUser = userRepository.findBySocialTypeAndSocialId(SocialType.APPLE, appleUserId);

            if (existingUser.isPresent()) {
                return appleLoginService.handleLogin(appleRefreshToken, existingUser.get(), response);
            } else {
                return appleLoginService.handleRegister(appleRefreshToken, oAuthAppleUserDto, response);
            }

        } catch (Exception e) {
            log.error("Apple login failed", e);
            throw new AppleLoginException();
        }
    }

    private String exchangeAppleRefreshToken(OAuthAppleRequestDto oAuthAppleRequestDto) {
        try {
            AppleTokenResponseDto tokenResponse = appleLoginService.getAppleAccessTokenAndRefreshToken(
                    oAuthAppleRequestDto.getAuthCode()
            );
            return tokenResponse.getRefreshToken();
        } catch (Exception e) {
            log.warn("Apple token exchange failed; continuing with verified identity token", e);
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        if (failed instanceof RequestValidationException) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":\"error\", \"message\":\"Apple 로그인 실패\"}");
    }

    private static class RequestValidationException extends AuthenticationException {
        private RequestValidationException() {
            super("Request validation failed");
        }
    }

    private static class AppleLoginException extends AuthenticationException {
        private AppleLoginException() {
            super("Apple login failed");
        }
    }

}
