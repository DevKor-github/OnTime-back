package devkor.ontime_back.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserAlarmSettingTest {

    @Test
    void defaultForEnablesNativeAlarmBridgeWithDefaultOffset() {
        User user = User.builder().id(1L).email("user@example.com").build();

        UserAlarmSetting setting = UserAlarmSetting.defaultFor(user);

        assertThat(setting.getUser()).isSameAs(user);
        assertThat(setting.getAlarmsEnabled()).isTrue();
        assertThat(setting.getDefaultAlarmOffsetMinutes()).isEqualTo(UserAlarmSetting.DEFAULT_ALARM_OFFSET_MINUTES);
        assertThat(setting.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateChangesOnlyProvidedAlarmPreferencesAndRefreshesTimestamp() {
        UserAlarmSetting setting = UserAlarmSetting.builder()
                .user(User.builder().id(1L).build())
                .alarmsEnabled(true)
                .defaultAlarmOffsetMinutes(5)
                .updatedAt(Instant.parse("2026-05-05T00:00:00Z"))
                .build();

        setting.update(false, null);
        Instant afterFirstUpdate = setting.getUpdatedAt();
        setting.update(null, 30);

        assertThat(setting.getAlarmsEnabled()).isFalse();
        assertThat(setting.getDefaultAlarmOffsetMinutes()).isEqualTo(30);
        assertThat(afterFirstUpdate).isAfter(Instant.parse("2026-05-05T00:00:00Z"));
        assertThat(setting.getUpdatedAt()).isAfterOrEqualTo(afterFirstUpdate);
    }
}
