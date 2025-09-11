package devkor.ontime_back.global.oauth.apple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.AppleTokenResponseDto;
import devkor.ontime_back.dto.OAuthAppleUserDto;
import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.global.jwt.JwtTokenProvider;
import devkor.ontime_back.global.jwt.JwtUtils;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
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
import java.time.Instant;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class AppleLoginService {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String REDIRECT_URI = "https://ontime.devkor.club/oauth2/apple/callback";
    private String issuer = "https://appleid.apple.com";
    @Value("${apple.client.id}")
    private String clientId;
    @Value("${apple.team.id}")
    private String teamId;
    @Value("${apple.login.key}")
    private String keyId;
    @Value("${apple.client.secret}")
    private String privateKeyPath;

    private final ApplePublicKeyGenerator applePublicKeyGenerator;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private final RestTemplate restTemplate = new RestTemplate();
    public Authentication handleLogin(String appleRefreshToken, User user, HttpServletResponse response) throws IOException {
        log.info("handleLogin");
        user.updateSocialLoginToken(appleRefreshToken);

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

    public Authentication handleRegister(String appleRefreshToken, OAuthAppleUserDto oAuthAppleUserDto, HttpServletResponse response) throws IOException {
        log.info("handleRegister");

        User newUser = User.builder()
                .socialType(SocialType.APPLE)
                .socialId(oAuthAppleUserDto.getAppleUserId())
                .email(oAuthAppleUserDto.getEmail())
                .name(oAuthAppleUserDto.getFullName())
                .role(Role.GUEST)
                .socialLoginToken(appleRefreshToken)
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

    // identitytoken 검증
    public Claims verifyIdentityToken(String identityToken) throws
            Exception {
        log.info("verifyIdentityToken");
        Map<String, String> headers = jwtUtils.parseHeaders(identityToken);
        // apple publickey
        ApplePublicKeyResponse applePublicKeyResponse = restTemplate.getForObject(APPLE_KEYS_URL, ApplePublicKeyResponse.class);
        PublicKey publicKey = applePublicKeyGenerator.generatePublicKey(headers, applePublicKeyResponse);
        // claim
        Claims tokenClaims = jwtUtils.getTokenClaims(identityToken, publicKey);
        // iss 확인
        if (!issuer.equals(tokenClaims.getIssuer())) {
            throw new InvalidTokenException("유효하지 않은 JWT입니다. issuer가 일치하지 않습니다.");
        }
        // aud 확인
        if (!clientId.equals(tokenClaims.getAudience())) {
            throw new InvalidTokenException("유효하지 않은 JWT입니다. audience가 일치하지 않습니다.");
        }
        // exp(만료 시간) 확인
        Date expiration = tokenClaims.getExpiration();
        if (expiration == null || expiration.before(Date.from(Instant.now()))) {
            throw new InvalidTokenException("유효하지 않은 JWT입니다. 만료되었습니다.");
        }

        return tokenClaims;
    }

    // apple 서버로부터 accesstoken, refreshtoken 발급
    public AppleTokenResponseDto getAppleAccessTokenAndRefreshToken(String authCode) throws Exception {
        // clientSecret
        String clientSecret = generateClientSecret();
        log.info("getAppleAccessTokenAndRefreshToken");
        log.info("client_id: {}", clientId);
        log.info("client_secret: {}", clientSecret);
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", authCode);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("redirect_uri", REDIRECT_URI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                APPLE_TOKEN_URL, HttpMethod.POST, requestEntity, JsonNode.class);

        JsonNode response = responseEntity.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.treeToValue(response, AppleTokenResponseDto.class);
    }

    // clientsecret 생성
    private String generateClientSecret() throws Exception {
        log.info("generageClientSecret");
        // Private Key
        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PrivateKey privateKey = kf.generatePrivate(spec);

        // now
        Date now = new Date();

        // clientSecret
        return Jwts.builder()
                .setHeaderParam("alg", "ES256")
                .setHeaderParam("kid", keyId)
                .setIssuer(teamId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 3600000))
                .setAudience("https://appleid.apple.com")
                .setSubject(clientId)
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();
    }
    public boolean revokeToken(Long userId) throws Exception {
        log.info("checkAppleLoginRevoked");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        String appleRefreshToken = user.getSocialLoginToken();

        String clientSecret = generateClientSecret();
        String revokeUrl = "https://appleid.apple.com/auth/revoke";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String requestBody = UriComponentsBuilder.newInstance()
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("token", appleRefreshToken)
                .queryParam("token_type_hint", "refresh_token")
                .build()
                .toString().substring(1);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    revokeUrl, HttpMethod.POST, requestEntity, String.class);
            return response.getStatusCode() != HttpStatus.OK; // -> 토큰이 아직 유효함
        } catch (HttpClientErrorException e) {
            return true; // 요청 실패 -> 이미 철회된 refreshToken
        }
    }
}

