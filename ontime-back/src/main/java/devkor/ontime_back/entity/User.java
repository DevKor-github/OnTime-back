package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String email; // 이메일

    private String password; // 비밀번호

    private String imageUrl; // 프로필 이미지

    @Column(length = 30)
    private String name; // 이름

    private Integer spareTime; // 여유시간

    @Lob // 대용량 텍스트 필드
    @Column(columnDefinition = "TEXT") // 명시적으로 TEXT 타입으로 정의
    private String note; // 주의사항

    private Float punctualityScore; // 성실도 점수

    private Integer scheduleCountAfterReset; // 성실도 점수 초기화 이후 약속 개수

    private Integer latenessCountAfterReset; // 성실도 점수 초기화 이후 지각한 약속 개수

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private SocialType socialType; // KAKAO, NAVER, GOOGLE

    private String socialId; // 로그인한 소셜 타입의 식별자 값 (일반 로그인인 경우 null)

    private String refreshToken; // refreshToken

    private String firebaseToken;

    private String socialLoginToken;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
    private UserSetting userSetting;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "requesterId", cascade = CascadeType.ALL)
    private List<FriendShip> requestedFriendship;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "receiverId", cascade = CascadeType.ALL)
    private List<FriendShip> receivedFriendship;

    public void updateAdditionalInfo(Integer spareTime, String note) {
        this.spareTime = spareTime;
        this.note = note;
    }

    public void authorizeUser() {
        this.role = Role.USER;
    }


    // 비밀번호 암호화
    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    public void updatePassword(String updatePassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(updatePassword);
    }

    public void updateRefreshToken(String updateRefreshToken) {
        this.refreshToken = updateRefreshToken;
    }

    //성실도 점수 초기화
    public void resetPunctualityScore() {
        this.punctualityScore = (float) -1;
        this.scheduleCountAfterReset = 0;
        this.latenessCountAfterReset = 0;
    }

    public void updateSocialLoginToken(String refreshToken) {
        this.socialLoginToken = refreshToken;
    }

    //여유 시간 업데이트
    public void updateSpareTime(Integer newSpareTime) { this.spareTime = newSpareTime; }

    //유저세팅과 연결
    public void setUserSetting(UserSetting userSetting) {
        this.userSetting = userSetting;
    }

    // 약속 수 초기화 및 설정
    public void setScheduleCountAfterReset(Integer scheduleCount) {
        this.scheduleCountAfterReset = scheduleCount;
    }

    // 지각 수 초기화 및 설정
    public void setLatenessCountAfterReset(Integer latenessCount) {
        this.latenessCountAfterReset = latenessCount;
    }

    public void setPunctualityScore(float punctualityScore) {
        this.punctualityScore = punctualityScore;
    }

    public void updateNote(String note) {
        this.note = note;
    }

    public void updateFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }
}