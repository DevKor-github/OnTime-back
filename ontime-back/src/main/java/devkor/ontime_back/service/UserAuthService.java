package devkor.ontime_back.service;

import devkor.ontime_back.dto.UserAdditionalInfoDto;
import devkor.ontime_back.dto.UserSignUpDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.entity.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 자체 로그인 회원가입
    public void signUp(UserSignUpDto userSignUpDto) throws Exception {

        if (userRepository.findByEmail(userSignUpDto.getEmail()).isPresent()) {
            throw new Exception("이미 존재하는 이메일입니다.");
        }

        if (userRepository.findByName(userSignUpDto.getName()).isPresent()) {
            throw new Exception("이미 존재하는 닉네임입니다.");
        }

        // 자체 로그인시, USER로 설정
        User user = User.builder()
                .id(userSignUpDto.getId())
                .email(userSignUpDto.getEmail())
                .password(userSignUpDto.getPassword())
                .name(userSignUpDto.getName())
                .role(Role.USER)
                .build();

        // 비밀번호 암호화 후 저장
        user.passwordEncode(passwordEncoder);
        userRepository.save(user);
    }

    public void addInfo(Long id, UserAdditionalInfoDto userAdditionalInfoDto) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("없는 유저 id임"));
        user.setSpareTime(userAdditionalInfoDto.getSpareTime());
        user.setNote(userAdditionalInfoDto.getNote());
        userRepository.save(user);
    }


}