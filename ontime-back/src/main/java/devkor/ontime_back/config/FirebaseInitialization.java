package devkor.ontime_back.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class FirebaseInitialization {
    private static final String DEFAULT_FIREBASE_CLASSPATH_RESOURCE =
            "ontime-c63f1-firebase-adminsdk-fbsvc-a043cdc829.json";

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.credentials.classpath:" + DEFAULT_FIREBASE_CLASSPATH_RESOURCE + "}")
    private String firebaseCredentialsClasspath;

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (InputStream serviceAccount = openServiceAccount()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            log.error("Failed to initialize Firebase credentials", e);
        }
    }

    private InputStream openServiceAccount() throws IOException {
        if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isBlank()) {
            Path credentialsPath = Path.of(firebaseCredentialsPath);
            if (!Files.exists(credentialsPath)) {
                throw new FileNotFoundException("Firebase credentials file not found: " + credentialsPath);
            }
            return Files.newInputStream(credentialsPath);
        }

        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(firebaseCredentialsClasspath);
        if (serviceAccount == null) {
            throw new FileNotFoundException("Firebase credentials resource not found: " + firebaseCredentialsClasspath);
        }
        return serviceAccount;
    }
}
