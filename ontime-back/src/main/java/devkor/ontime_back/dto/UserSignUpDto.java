package devkor.ontime_back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class UserSignUpDto {
    // 자체 로그인 회원 가입 API에 RequestBody로 사용할 UserSignUpDto를 생성
    private String email;
    private String password;
    private String name;
}
