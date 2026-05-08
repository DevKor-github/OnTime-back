package devkor.ontime_back.security;

import devkor.ontime_back.config.ProductionSecretValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecretValidatorTest {

    @Test
    void validProductionSecretsPass() {
        ProductionSecretValidator validator = new ProductionSecretValidator(validEnvironment());

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void missingRequiredSecretsFailStartup() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty("firebase.credentials.base64", "");

        ProductionSecretValidator validator = new ProductionSecretValidator(environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("firebase.credentials.base64 is required");
    }

    @Test
    void placeholderValuesFailStartup() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty("apple.client.id", "your_apple_client_id");

        ProductionSecretValidator validator = new ProductionSecretValidator(environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apple.client.id must not use placeholder or sample values");
    }

    @Test
    void rootDatabaseUserFailsStartup() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty("spring.datasource.username", "root");

        ProductionSecretValidator validator = new ProductionSecretValidator(environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.username must not be root");
    }

    @Test
    void weakJwtSecretFailsStartup() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty("jwt.secret.key", "short-jwt-key");

        ProductionSecretValidator validator = new ProductionSecretValidator(environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret.key must be at least 64 characters");
    }

    @Test
    void legacyFileBasedSecretSourcesFailStartup() {
        MockEnvironment environment = validEnvironment();
        environment.setProperty("apple.client.secret", "/app/secrets/AuthKey.p8");
        environment.setProperty("firebase.credentials.path", "/app/secrets/firebase-adminsdk.json");

        ProductionSecretValidator validator = new ProductionSecretValidator(environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apple.client.secret is not allowed in prod")
                .hasMessageContaining("firebase.credentials.path is not allowed in prod");
    }

    private MockEnvironment validEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:mysql://prod-db.ontime.internal:3306/ontime?sslMode=REQUIRED&serverTimezone=UTC");
        environment.setProperty("spring.datasource.username", "ontime_app");
        environment.setProperty("spring.datasource.password", "rotatedDatabasePasswordValue000000000000");
        environment.setProperty("jwt.secret.key", "rotatedJwtSigningKeyValue000000000000000000000000000000000000000000");
        environment.setProperty("google.web.client-id", "rotated-google-web-client-id.apps.googleusercontent.com");
        environment.setProperty("google.app.client-id", "rotated-google-app-client-id.apps.googleusercontent.com");
        environment.setProperty("spring.security.oauth2.client.registration.google.client-secret", "rotatedGoogleOauthClientValue000000000000");
        environment.setProperty("apple.client.id", "club.devkor.ontime.service");
        environment.setProperty("apple.team.id", "TEAMID0000");
        environment.setProperty("apple.login.key", "APPLEKEY00");
        environment.setProperty("apple.private-key.base64", "rotatedApplePrivateKeyBase64Value000000000000");
        environment.setProperty("firebase.credentials.base64", "rotatedFirebaseCredentialsBase64Value000000000000");
        return environment;
    }
}
