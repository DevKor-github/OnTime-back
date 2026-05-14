package devkor.ontime_back.global.oauth.apple;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplePublicKeyGeneratorTest {

    private final ApplePublicKeyGenerator generator = new ApplePublicKeyGenerator();

    @Test
    void generatePublicKeySelectsMatchingAppleKeyAndReconstructsRsaPublicKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        RSAPublicKey expectedKey = (RSAPublicKey) keyPair.getPublic();
        ApplePublicKey matchingKey = applePublicKey("matched-key", "RS256", expectedKey);
        ApplePublicKeyResponse response = new ApplePublicKeyResponse(List.of(
                applePublicKey("other-key", "RS256", expectedKey),
                matchingKey
        ));

        RSAPublicKey actualKey = (RSAPublicKey) generator.generatePublicKey(
                Map.of("kid", "matched-key", "alg", "RS256"),
                response
        );

        assertThat(actualKey.getModulus()).isEqualTo(expectedKey.getModulus());
        assertThat(actualKey.getPublicExponent()).isEqualTo(expectedKey.getPublicExponent());
    }

    @Test
    void applePublicKeyResponseRejectsTokensWithoutMatchingKidAndAlgorithm() {
        ApplePublicKeyResponse response = new ApplePublicKeyResponse(List.of(
                new ApplePublicKey("RSA", "key-1", "RS256", "AQAB", "AQAB")
        ));

        assertThatThrownBy(() -> response.getMatchedKey("key-2", "RS256"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JWT: No matching Apple Public Key found");
    }

    private ApplePublicKey applePublicKey(String kid, String alg, RSAPublicKey key) {
        return new ApplePublicKey(
                "RSA",
                kid,
                alg,
                Base64.getUrlEncoder().withoutPadding().encodeToString(key.getModulus().toByteArray()),
                Base64.getUrlEncoder().withoutPadding().encodeToString(key.getPublicExponent().toByteArray())
        );
    }
}
