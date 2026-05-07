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
                .containsEntry("spring.jpa.hibernate.ddl-auto", "update")
                .containsEntry("spring.jpa.show-sql", "true")
                .containsEntry("spring.jpa.properties.hibernate.format_sql", "true");
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
