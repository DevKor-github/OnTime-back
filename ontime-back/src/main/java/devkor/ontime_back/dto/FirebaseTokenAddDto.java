package devkor.ontime_back.dto;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class FirebaseTokenAddDto {
    String firebaseToken;
    String deviceId;
}
