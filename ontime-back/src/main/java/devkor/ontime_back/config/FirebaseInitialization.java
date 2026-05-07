package devkor.ontime_back.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
@Slf4j
public class FirebaseInitialization {

    @Value("${firebase.credentials.base64:}")
    private String firebaseCredentialsBase64;

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${google.application.credentials:}")
    private String googleApplicationCredentials;

    @PostConstruct
    public void initialize() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            try (InputStream serviceAccount = resolveCredentials()) {
                if (serviceAccount == null) {
                    log.warn("Firebase credentials were not provided; Firebase push notifications are disabled.");
                    return;
                }

                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase.", e);
        }
    }

    private InputStream resolveCredentials() throws IOException {
        if (hasText(firebaseCredentialsBase64)) {
            byte[] decodedCredentials = Base64.getDecoder().decode(firebaseCredentialsBase64);
            return new ByteArrayInputStream(decodedCredentials);
        }

        if (hasText(firebaseCredentialsJson)) {
            return new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        String credentialsPath = hasText(firebaseCredentialsPath)
                ? firebaseCredentialsPath
                : googleApplicationCredentials;
        if (hasText(credentialsPath)) {
            return Files.newInputStream(Path.of(credentialsPath));
        }

        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
