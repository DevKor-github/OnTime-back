package devkor.ontime_back.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionSecretValidator {

    private final Environment environment;

    @PostConstruct
    public void validate() {
        List<String> errors = new ArrayList<>();

        requireSecret(errors, "spring.datasource.url");
        requireSecret(errors, "spring.datasource.username");
        requireSecret(errors, "spring.datasource.password");
        requireSecret(errors, "jwt.secret.key");
        requireSecret(errors, "google.web.client-id");
        requireSecret(errors, "google.app.client-id");
        requireSecret(errors, "spring.security.oauth2.client.registration.google.client-secret");
        requireSecret(errors, "apple.client.id");
        requireSecret(errors, "apple.team.id");
        requireSecret(errors, "apple.login.key");
        requireSecret(errors, "apple.private-key.base64");
        requireSecret(errors, "firebase.credentials.base64");

        validateDatabase(errors);
        validateJwt(errors);
        rejectLegacySecretSource(errors, "apple.client.secret");
        rejectLegacySecretSource(errors, "apple.private-key");
        rejectLegacySecretSource(errors, "firebase.credentials.json");
        rejectLegacySecretSource(errors, "firebase.credentials.path");
        rejectLegacySecretSource(errors, "google.application.credentials");

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe production secret configuration: " + String.join("; ", errors));
        }
    }

    private void validateDatabase(List<String> errors) {
        String username = property("spring.datasource.username");
        if ("root".equalsIgnoreCase(username)) {
            errors.add("spring.datasource.username must not be root");
        }

        String databaseUrl = property("spring.datasource.url").toLowerCase(Locale.ROOT);
        if (databaseUrl.contains("allowpublickeyretrieval=true")) {
            errors.add("spring.datasource.url must not enable allowPublicKeyRetrieval");
        }
        if (databaseUrl.contains("createdatabaseifnotexist=true")) {
            errors.add("spring.datasource.url must not create databases at startup");
        }
        if (databaseUrl.contains("usessl=false")) {
            errors.add("spring.datasource.url must not disable TLS");
        }
    }

    private void validateJwt(List<String> errors) {
        String jwtSecret = property("jwt.secret.key");
        if (hasText(jwtSecret) && jwtSecret.length() < 64) {
            errors.add("jwt.secret.key must be at least 64 characters");
        }
    }

    private void requireSecret(List<String> errors, String propertyName) {
        String value = property(propertyName);
        if (!hasText(value)) {
            errors.add(propertyName + " is required");
            return;
        }

        if (looksLikePlaceholder(value)) {
            errors.add(propertyName + " must not use placeholder or sample values");
        }
    }

    private void rejectLegacySecretSource(List<String> errors, String propertyName) {
        if (hasText(property(propertyName))) {
            errors.add(propertyName + " is not allowed in prod; use the base64 environment secret");
        }
    }

    private String property(String propertyName) {
        try {
            return environment.getProperty(propertyName, "");
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean looksLikePlaceholder(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("your_")
                || normalized.contains("your-")
                || normalized.contains("change-me")
                || normalized.contains("changeme")
                || normalized.contains("placeholder")
                || normalized.contains("dummy")
                || normalized.contains("fake")
                || normalized.contains("sample")
                || normalized.contains("example")
                || normalized.startsWith("test-")
                || normalized.startsWith("test_")
                || normalized.contains("test_secret")
                || normalized.contains("my_secret_key_for_ontime_back_application_development_environment")
                || normalized.contains("ontime1234");
    }
}
