package devkor.ontime_back.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileMutationTest {

    @Test
    void userProfileMutatorsUpdatePersistedAuthenticationAndProfileState() {
        User user = User.builder()
                .email("user@example.com")
                .password("raw-password")
                .role(Role.GUEST)
                .build();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        user.passwordEncode(passwordEncoder);
        user.updatePassword("new-password", passwordEncoder);
        user.updateAdditionalInfo(15, "leave early");
        user.authorizeUser();
        user.updateNote("bring laptop");
        user.updateFirebaseToken("firebase-token");
        user.updateAccessToken("access-token");
        user.updateRefreshToken("refresh-token");
        user.updateSocialLoginToken("provider-refresh-token");

        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
        assertThat(user.getSpareTime()).isEqualTo(15);
        assertThat(user.getNote()).isEqualTo("bring laptop");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getFirebaseToken()).isEqualTo("firebase-token");
        assertThat(user.getAccessToken()).isEqualTo("access-token");
        assertThat(user.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getSocialLoginToken()).isEqualTo("provider-refresh-token");
    }
}
