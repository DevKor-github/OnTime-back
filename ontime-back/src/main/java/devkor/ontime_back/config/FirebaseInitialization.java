package devkor.ontime_back.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class FirebaseInitialization {

    private static final String DEFAULT_FIREBASE_RESOURCE = "ontime-c63f1-firebase-adminsdk-fbsvc-a043cdc829.json";

    @Value("${firebase.service-account.path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void initialize() {
        try (InputStream serviceAccount = openServiceAccount()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            } else {
                log.info("Firebase already initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }

    private InputStream openServiceAccount() throws IOException {
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            return new FileInputStream(serviceAccountPath);
        }

        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(DEFAULT_FIREBASE_RESOURCE);
        if (serviceAccount == null) {
            throw new FileNotFoundException("Resource not found: " + DEFAULT_FIREBASE_RESOURCE);
        }
        return serviceAccount;
    }
}
