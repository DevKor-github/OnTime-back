package devkor.ontime_back.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseInitializationTest {

    @TempDir
    private Path tempDir;

    @Test
    void resolveCredentialsPrefersBase64EncodedCredentials() throws Exception {
        FirebaseInitialization initialization = new FirebaseInitialization();
        ReflectionTestUtils.setField(
                initialization,
                "firebaseCredentialsBase64",
                Base64.getEncoder().encodeToString("base64-json".getBytes(StandardCharsets.UTF_8))
        );
        ReflectionTestUtils.setField(initialization, "firebaseCredentialsJson", "inline-json");

        try (InputStream credentials = ReflectionTestUtils.invokeMethod(initialization, "resolveCredentials")) {
            assertThat(new String(credentials.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("base64-json");
        }
    }

    @Test
    void resolveCredentialsFallsBackToInlineJson() throws Exception {
        FirebaseInitialization initialization = new FirebaseInitialization();
        ReflectionTestUtils.setField(initialization, "firebaseCredentialsJson", "inline-json");

        try (InputStream credentials = ReflectionTestUtils.invokeMethod(initialization, "resolveCredentials")) {
            assertThat(new String(credentials.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("inline-json");
        }
    }

    @Test
    void resolveCredentialsUsesConfiguredPathBeforeGoogleApplicationCredentials() throws Exception {
        Path configuredPath = tempDir.resolve("firebase.json");
        Path googlePath = tempDir.resolve("google.json");
        java.nio.file.Files.writeString(configuredPath, "configured-path-json");
        java.nio.file.Files.writeString(googlePath, "google-path-json");
        FirebaseInitialization initialization = new FirebaseInitialization();
        ReflectionTestUtils.setField(initialization, "firebaseCredentialsPath", configuredPath.toString());
        ReflectionTestUtils.setField(initialization, "googleApplicationCredentials", googlePath.toString());

        try (InputStream credentials = ReflectionTestUtils.invokeMethod(initialization, "resolveCredentials")) {
            assertThat(new String(credentials.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("configured-path-json");
        }
    }

    @Test
    void resolveCredentialsReturnsNullWhenNoCredentialsAreConfigured() throws Exception {
        FirebaseInitialization initialization = new FirebaseInitialization();

        InputStream credentials = ReflectionTestUtils.invokeMethod(initialization, "resolveCredentials");

        assertThat(credentials).isNull();
    }
}
