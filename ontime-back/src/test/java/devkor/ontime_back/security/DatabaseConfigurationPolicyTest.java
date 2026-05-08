package devkor.ontime_back.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationPolicyTest {

    private static final Path RESOURCES_ROOT = Path.of("src/main/resources");

    @Test
    void productionLikeProfilesDoNotMutateSchemaOrLogSql() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertThat(properties)
                .as("prod profile database safety")
                .containsEntry("spring.datasource.url", "${SPRING_DATASOURCE_URL}")
                .containsEntry("spring.datasource.username", "${SPRING_DATASOURCE_USERNAME}")
                .containsEntry("spring.datasource.password", "${SPRING_DATASOURCE_PASSWORD}")
                .containsEntry("spring.jpa.hibernate.ddl-auto", "validate")
                .containsEntry("spring.jpa.show-sql", "false")
                .containsEntry("spring.jpa.properties.hibernate.format_sql", "false")
                .containsEntry("spring.flyway.enabled", "true")
                .containsEntry("spring.flyway.baseline-on-migrate", "false");
    }

    @Test
    void productionLikeProfilesDoNotContainFallbackCredentialsOrUnsafeJdbcFlags() throws IOException {
        String content = Files.readString(RESOURCES_ROOT.resolve("application-prod.properties"));
        String normalizedContent = content.toLowerCase(Locale.ROOT);

        assertThat(normalizedContent)
                .as("prod profile must not include unsafe production defaults")
                .doesNotContain(":root")
                .doesNotContain("ontime1234")
                .doesNotContain("allowpublickeyretrieval=true")
                .doesNotContain("createdatabaseifnotexist=true")
                .doesNotContain("usessl=false");
    }

    @Test
    void localProfileKeepsDeveloperOnlySchemaUpdateDefaults() throws IOException {
        Properties properties = loadProperties("application-local.properties");

        assertThat(properties)
                .containsEntry("spring.datasource.username", "${SPRING_DATASOURCE_USERNAME:ontime_local}")
                .containsEntry("spring.datasource.password", "${SPRING_DATASOURCE_PASSWORD:local_dev_password_not_for_prod}")
                .containsEntry("spring.jpa.hibernate.ddl-auto", "update")
                .containsEntry("spring.jpa.show-sql", "true")
                .containsEntry("spring.jpa.properties.hibernate.format_sql", "true")
                .containsEntry("feature.apple-login.enabled", "${FEATURE_APPLE_LOGIN_ENABLED:false}");
    }

    @Test
    void productionProfileDeclaresExternalSecretInputsWithoutDefaults() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertThat(properties)
                .containsEntry("jwt.secret.key", "${JWT_SECRET_KEY}")
                .containsEntry("google.web.client-id", "${GOOGLE_WEB_CLIENT_ID}")
                .containsEntry("google.app.client-id", "${GOOGLE_APP_CLIENT_ID}")
                .containsEntry("spring.security.oauth2.client.registration.google.client-secret", "${SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET}")
                .containsEntry("apple.client.id", "${APPLE_CLIENT_ID}")
                .containsEntry("apple.team.id", "${APPLE_TEAM_ID}")
                .containsEntry("apple.login.key", "${APPLE_LOGIN_KEY}")
                .containsEntry("apple.private-key.base64", "${APPLE_PRIVATE_KEY_BASE64}")
                .containsEntry("firebase.credentials.base64", "${FIREBASE_CREDENTIALS_BASE64}");
    }

    @Test
    void deployWorkflowPinsAndValidatesProductionDatabaseSafety() throws IOException {
        String workflow = Files.readString(repoRoot().resolve(".github/workflows/deploy.yml"));

        assertThat(workflow)
                .contains("SPRING_JPA_HIBERNATE_DDL_AUTO=validate")
                .contains("SPRING_FLYWAY_BASELINE_ON_MIGRATE=false")
                .contains("SPRING_DATASOURCE_URL is required.")
                .contains("SPRING_DATASOURCE_USERNAME is required.")
                .contains("SPRING_DATASOURCE_PASSWORD is required.")
                .contains("SPRING_DATASOURCE_USERNAME must not be root.")
                .contains("allowpublickeyretrieval=true")
                .contains("createdatabaseifnotexist=true")
                .contains("usessl=false")
                .contains("sslmode=required")
                .doesNotContain("SPRING_JPA_HIBERNATE_DDL_AUTO=${{ secrets.SPRING_JPA_HIBERNATE_DDL_AUTO }}")
                .doesNotContain("SPRING_FLYWAY_BASELINE_ON_MIGRATE=true");
    }

    @Test
    void deployWorkflowSuppliesProductionSecretEnvironment() throws IOException {
        String workflow = Files.readString(repoRoot().resolve(".github/workflows/deploy.yml"));

        assertThat(workflow)
                .contains("SPRING_PROFILES_ACTIVE=prod")
                .contains("SPRING_DATASOURCE_URL=${{ secrets.SPRING_DATASOURCE_URL }}")
                .contains("SPRING_DATASOURCE_USERNAME=${{ secrets.SPRING_DATASOURCE_USERNAME }}")
                .contains("SPRING_DATASOURCE_PASSWORD=${{ secrets.SPRING_DATASOURCE_PASSWORD }}")
                .contains("JWT_SECRET_KEY=${{ secrets.JWT_SECRETKEY }}")
                .contains("JWT_ACCESS_EXPIRATION=${{ secrets.JWT_ACCESS_EXPIRATION }}")
                .contains("JWT_REFRESH_EXPIRATION=${{ secrets.JWT_REFRESH_EXPIRATION }}")
                .contains("JWT_ACCESS_HEADER=${{ secrets.JWT_ACCESS_HEADER }}")
                .contains("JWT_REFRESH_HEADER=${{ secrets.JWT_REFRESH_HEADER }}")
                .contains("GOOGLE_WEB_CLIENT_ID=${{ secrets.GOOGLE_WEB_CLIENT_ID }}")
                .contains("GOOGLE_APP_CLIENT_ID=${{ secrets.GOOGLE_APP_CLIENT_ID }}")
                .contains("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=${{ secrets.SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET }}")
                .contains("APPLE_CLIENT_ID=${{ secrets.APPLE_CLIENT_ID }}")
                .contains("APPLE_TEAM_ID=${{ secrets.APPLE_TEAM_ID }}")
                .contains("APPLE_LOGIN_KEY=${{ secrets.APPLE_LOGIN_KEY }}")
                .contains("APPLE_PRIVATE_KEY_BASE64=${{ secrets.APPLE_PRIVATE_KEY_BASE64 }}")
                .contains("FIREBASE_CREDENTIALS_BASE64=${{ secrets.FIREBASE_CREDENTIALS_BASE64 }}")
                .doesNotContain("APPLE_CLIENT_SECRET=")
                .doesNotContain("FIREBASE_CREDENTIALS_PATH=")
                .doesNotContain("GOOGLE_APPLICATION_CREDENTIALS=");
    }

    @Test
    void testWorkflowUsesTrackedTestProfileInsteadOfGeneratingIgnoredApplicationProperties() throws IOException {
        String workflow = Files.readString(repoRoot().resolve(".github/workflows/test.yml"));

        assertThat(workflow)
                .contains("SPRING_PROFILES_ACTIVE: test")
                .doesNotContain("Create Config Files")
                .doesNotContain("ontime-back/src/main/resources/application.properties");
    }

    private Properties loadProperties(String fileName) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(RESOURCES_ROOT.resolve(fileName))) {
            properties.load(inputStream);
        }
        return properties;
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.exists(current.resolve(".github/workflows/deploy.yml"))) {
            return current;
        }
        return current.getParent();
    }
}
