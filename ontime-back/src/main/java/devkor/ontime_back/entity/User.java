package devkor.ontime_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.sql.Time;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "user_id")
    private UUID id;

    private String email; // 이메일

    private String password; // 비밀번호

    private String imageUrl; // 프로필 이미지

    @Column(length = 30)
    private String name; // 이름

    private Time spareTime; // 여유시간

    @Lob // 대용량 텍스트 필드
    @Column(columnDefinition = "TEXT") // 명시적으로 TEXT 타입으로 정의
    private String note; // 주의사항

    private Float score; // 성실도 점수

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private SocialType socialType; // KAKAO, NAVER, GOOGLE

    private String socialId; // 로그인한 소셜 타입의 식별자 값 (일반 로그인인 경우 null)

    private String refreshToken; // refreshToken

    public void updateAdditionalInfo(Time spareTime, String note, Float score) {
        this.spareTime = spareTime;
        this.note = note;
        this.score = score;
    }

    public void authorizeUser() {
        this.role = Role.USER;
    }


    // 비밀번호 암호화
    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    public void updateName(String updateName) {
        this.name = updateName;
    }

    public void updatePassword(String updatePassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(updatePassword);
    }

    public void updateRefreshToken(String updateRefreshToken) {
        this.refreshToken = updateRefreshToken;
    }
}