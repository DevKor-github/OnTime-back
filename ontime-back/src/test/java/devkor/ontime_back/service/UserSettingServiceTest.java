package devkor.ontime_back.service;

import devkor.ontime_back.dto.UserSettingUpdateDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserSetting;
import devkor.ontime_back.repository.UserSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingServiceTest {

    @Mock
    private UserSettingRepository userSettingRepository;

    private UserSettingService userSettingService;

    @BeforeEach
    void setUp() {
        userSettingService = new UserSettingService(userSettingRepository);
    }

    @Test
    void updateSettingPersistsAllUserNotificationPreferences() {
        UserSetting setting = userSetting(true, 50, true, true);
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(setting));
        UserSettingUpdateDto request = updateDto(false, 25, false, false);

        userSettingService.updateSetting(1L, request);

        assertThat(setting.getIsNotificationsEnabled()).isFalse();
        assertThat(setting.getSoundVolume()).isEqualTo(25);
        assertThat(setting.getIsPlayOnSpeaker()).isFalse();
        assertThat(setting.getIs24HourFormat()).isFalse();
        verify(userSettingRepository).save(setting);
    }

    @Test
    void resetSettingRestoresDefaultNotificationPreferences() {
        UserSetting setting = userSetting(false, 5, false, false);
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(setting));

        userSettingService.resetSetting(1L);

        assertThat(setting.getIsNotificationsEnabled()).isTrue();
        assertThat(setting.getSoundVolume()).isEqualTo(50);
        assertThat(setting.getIsPlayOnSpeaker()).isTrue();
        assertThat(setting.getIs24HourFormat()).isTrue();
        verify(userSettingRepository).save(setting);
    }

    @Test
    void updateSettingFailsClearlyWhenUserHasNoSettings() {
        when(userSettingRepository.findByUserId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSettingService.updateSetting(404L, updateDto(true, 50, true, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UserSetting not found with given userId");
    }

    @Test
    void resetSettingFailsClearlyWhenUserHasNoSettings() {
        when(userSettingRepository.findByUserId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSettingService.resetSetting(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UserSetting not found with given userId");
    }

    private UserSettingUpdateDto updateDto(Boolean notificationsEnabled,
                                           Integer soundVolume,
                                           Boolean playOnSpeaker,
                                           Boolean twentyFourHourFormat) {
        UserSettingUpdateDto dto = new UserSettingUpdateDto();
        ReflectionTestUtils.setField(dto, "isNotificationsEnabled", notificationsEnabled);
        ReflectionTestUtils.setField(dto, "soundVolume", soundVolume);
        ReflectionTestUtils.setField(dto, "isPlayOnSpeaker", playOnSpeaker);
        ReflectionTestUtils.setField(dto, "is24HourFormat", twentyFourHourFormat);
        return dto;
    }

    private UserSetting userSetting(Boolean notificationsEnabled,
                                    Integer soundVolume,
                                    Boolean playOnSpeaker,
                                    Boolean twentyFourHourFormat) {
        return UserSetting.builder()
                .userSettingId(UUID.randomUUID())
                .user(User.builder().id(1L).build())
                .isNotificationsEnabled(notificationsEnabled)
                .soundVolume(soundVolume)
                .isPlayOnSpeaker(playOnSpeaker)
                .is24HourFormat(twentyFourHourFormat)
                .build();
    }
}
