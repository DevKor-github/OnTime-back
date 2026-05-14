package devkor.ontime_back.service;

import devkor.ontime_back.dto.FirebaseTokenAddDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseTokenServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlarmService alarmService;

    private FirebaseTokenService firebaseTokenService;

    @BeforeEach
    void setUp() {
        firebaseTokenService = new FirebaseTokenService(userRepository, alarmService);
    }

    @Test
    void registerFirebaseTokenUpdatesUserAndLinksCurrentDeviceWhenDeviceIdIsPresent() {
        User user = User.builder().id(1L).build();
        FirebaseTokenAddDto dto = firebaseTokenAddDto("firebase-token", "device-id-1234567");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        firebaseTokenService.registerFirebaseToken(1L, dto, "access-token");

        assertThat(user.getFirebaseToken()).isEqualTo("firebase-token");
        verify(alarmService).linkFirebaseToken(1L, "device-id-1234567", "firebase-token", "access-token");
        verify(userRepository).save(user);
    }

    @Test
    void registerFirebaseTokenDoesNotLinkBlankDeviceId() {
        User user = User.builder().id(1L).build();
        FirebaseTokenAddDto dto = firebaseTokenAddDto("firebase-token", " ");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        firebaseTokenService.registerFirebaseToken(1L, dto, "access-token");

        assertThat(user.getFirebaseToken()).isEqualTo("firebase-token");
        verifyNoInteractions(alarmService);
        verify(userRepository).save(user);
    }

    @Test
    void registerFirebaseTokenFailsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> firebaseTokenService.registerFirebaseToken(
                1L,
                firebaseTokenAddDto("firebase-token", "device-id-1234567"),
                "access-token"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void sendTestNotificationFailsClearlyWhenUserHasNoFirebaseTokenRecord() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> firebaseTokenService.sendTestNotification(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FirebaseToken not found");
    }

    private FirebaseTokenAddDto firebaseTokenAddDto(String firebaseToken, String deviceId) {
        FirebaseTokenAddDto dto = new FirebaseTokenAddDto();
        ReflectionTestUtils.setField(dto, "firebaseToken", firebaseToken);
        ReflectionTestUtils.setField(dto, "deviceId", deviceId);
        return dto;
    }
}
