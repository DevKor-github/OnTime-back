package devkor.ontime_back.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private final JwtUtils jwtUtils = new JwtUtils();

    @Test
    void parseHeadersDecodesJwtHeaderWithoutTrustingThePayload() throws Exception {
        String encodedHeader = Base64.getEncoder().encodeToString("{\"kid\":\"apple-key\",\"alg\":\"RS256\"}".getBytes());
        String token = encodedHeader + ".payload.signature";

        Map<String, String> headers = jwtUtils.parseHeaders(token);

        assertThat(headers).containsEntry("kid", "apple-key");
        assertThat(headers).containsEntry("alg", "RS256");
        assertThat(jwtUtils.decodeHeader(encodedHeader)).contains("apple-key");
    }

    @Test
    void getTokenClaimsVerifiesSignatureWithTheProvidedPublicKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String token = Jwts.builder()
                .setSubject("apple-user-id")
                .setIssuer("https://appleid.apple.com")
                .setAudience("com.ontime.service")
                .setExpiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        Claims claims = jwtUtils.getTokenClaims(token, keyPair.getPublic());

        assertThat(claims.getSubject()).isEqualTo("apple-user-id");
        assertThat(claims.getIssuer()).isEqualTo("https://appleid.apple.com");
        assertThat(claims.getAudience()).isEqualTo("com.ontime.service");
    }
}
