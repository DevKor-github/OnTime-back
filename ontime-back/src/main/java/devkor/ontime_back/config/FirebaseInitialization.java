package devkor.ontime_back.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class FirebaseInitialization {

    @PostConstruct
    public void initialize() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("firebaseApp is already exist 이미 있어 파베가. 이미 등록했어");
            }
//            FileInputStream serviceAccount =
//                    new FileInputStream("src/main/resources/ontime-c63f1-firebase-adminsdk-fbsvc-a043cdc829.json");

            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("ontime-c63f1-firebase-adminsdk-fbsvc-a043cdc829.json");
            if (serviceAccount == null) {
                log.error("json파일을 찾지 못했어요 ㅠㅠ damn 차라리 이거면 좋겠다. it should be");
                throw new FileNotFoundException("Resource not found: ontime-c63f1-firebase-adminsdk-fbsvc-a043cdc829.json");
            }

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase 초기화에 성공했습니다 하하하하 이러면 디버깅이 괴장히 어려워짐!");
        } catch (IOException e) {
            log.error("❌ Firebase 초기화 실패: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
