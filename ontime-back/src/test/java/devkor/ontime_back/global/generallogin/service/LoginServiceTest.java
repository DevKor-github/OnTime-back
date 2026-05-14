package devkor.ontime_back.global.generallogin.service;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    @Test
    void loadUserByUsernameReturnsSpringSecurityUserDetails() {
        UserRepository userRepository = mock(UserRepository.class);
        LoginService loginService = new LoginService(userRepository);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build()));

        org.springframework.security.core.userdetails.UserDetails userDetails =
                loginService.loadUserByUsername("user@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsernameFailsWhenEmailDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        LoginService loginService = new LoginService(userRepository);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("해당 이메일이 존재하지 않습니다.");
    }
}
