package devkor.ontime_back.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceSecretPackagingPolicyTest {

    @Test
    void sourceResourcesDoNotContainCredentialFiles() throws IOException {
        assertForbiddenFilesAbsent(Path.of("src/main/resources"));
    }

    @Test
    void processedResourcesDoNotContainCredentialFiles() throws IOException {
        Path processedResources = Path.of("build/resources/main");

        assertThat(Files.exists(processedResources))
                .as("Gradle should have processed main resources before tests run")
                .isTrue();
        assertForbiddenFilesAbsent(processedResources);
    }

    @Test
    void packagingGuardsExcludeCredentialFiles() throws IOException {
        String buildGradle = Files.readString(Path.of("build.gradle"));
        String dockerignore = Files.readString(Path.of(".dockerignore"));

        assertThat(buildGradle)
                .contains("tasks.named('processResources')")
                .contains("exclude '**/*.p8'")
                .contains("exclude '**/*.pem'")
                .contains("exclude '**/*.key'")
                .contains("exclude '**/*firebase*adminsdk*.json'")
                .contains("exclude 'application.properties'");

        assertThat(dockerignore)
                .contains("src/main/resources/application.properties")
                .contains("src/main/resources/**/*.p8")
                .contains("src/main/resources/**/*firebase*adminsdk*.json")
                .contains(".secrets");
    }

    @Test
    void backendRootDoesNotContainMobileFirebaseConfig() {
        assertThat(Files.exists(Path.of("GoogleService-Info.plist")))
                .as("Backend repo must not commit mobile Firebase config files")
                .isFalse();
    }

    private void assertForbiddenFilesAbsent(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            List<String> violations = files
                    .filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .filter(this::isForbiddenCredentialFile)
                    .toList();

            assertThat(violations)
                    .as(root + " must not contain private runtime credential files")
                    .isEmpty();
        }
    }

    private boolean isForbiddenCredentialFile(String relativePath) {
        String normalized = relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);

        return fileName.equals(".env")
                || fileName.equals("application.properties")
                || fileName.equals("googleservice-info.plist")
                || normalized.endsWith(".p8")
                || normalized.endsWith(".pem")
                || normalized.endsWith(".key")
                || (normalized.endsWith(".json")
                && (normalized.contains("firebase")
                || normalized.contains("adminsdk")
                || normalized.contains("service-account")
                || normalized.contains("service_account")));
    }
}
