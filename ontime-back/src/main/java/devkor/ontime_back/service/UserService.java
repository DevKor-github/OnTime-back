package devkor.ontime_back.service;

import devkor.ontime_back.dto.UserSignUpDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.entity.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 자체 로그인 회원가입
    public void signUp(UserSignUpDto userSignUpDto) throws Exception {

        if (userRepository.findByEmail(userSignUpDto.getEmail()).isPresent()) {
            throw new Exception("이미 존재하는 이메일입니다.");
        }

        if (userRepository.findByNickname(userSignUpDto.getNickname()).isPresent()) {
            throw new Exception("이미 존재하는 닉네임입니다.");
        }

        // 자체 로그인시, USER로 설정
        User user = User.builder()
                .email(userSignUpDto.getEmail())
                .password(userSignUpDto.getPassword())
                .nickname(userSignUpDto.getNickname())
                .role(Role.USER)
                .build();

        // 비밀번호 암호화 후 저장
        user.passwordEncode(passwordEncoder);
        userRepository.save(user);
    }
}