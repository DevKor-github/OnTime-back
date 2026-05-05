package devkor.ontime_back.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.AlarmDeviceCurrentRequestDto;
import devkor.ontime_back.dto.AlarmDeviceCurrentResponseDto;
import devkor.ontime_back.dto.AlarmStatusReportRequestDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.entity.UserAlarmStatus;
import devkor.ontime_back.entity.UserDevice;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserAlarmStatusRepository;
import devkor.ontime_back.repository.UserDeviceRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    private static final Long USER_ID = 1L;
    private static final String DEVICE_ID = "4f78cdd2-2d90-43b8-8bc5-53df8d9c5b12";
    private static final String ACCESS_TOKEN = "current-access-token";
    private static final String REFRESH_TOKEN = "current-refresh-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAlarmSettingRepository userAlarmSettingRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private UserAlarmStatusRepository userAlarmStatusRepository;

    private AlarmService alarmService;
    private User user;

    @BeforeEach
    void setUp() {
        alarmService = new AlarmService(
                userRepository,
                userAlarmSettingRepository,
                userDeviceRepository,
                userAlarmStatusRepository,
                new ObjectMapper()
        );
        user = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .accessToken(ACCESS_TOKEN)
                .build();
    }

    @Test
    @DisplayName("registerCurrentDevice binds the active device to the current access token and deactivates older devices")
    void registerCurrentDeviceBindsSessionAndDeactivatesOlderDevices() {
        UserDevice oldDevice = userDevice("old-device-id-1234", 10L);
        oldDevice.bindSession("old-access-token", null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userDeviceRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(oldDevice));
        when(userDeviceRepository.findByUserIdAndDeviceId(USER_ID, DEVICE_ID)).thenReturn(Optional.empty());
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlarmDeviceCurrentRequestDto request = AlarmDeviceCurrentRequestDto.builder()
                .deviceId(DEVICE_ID)
                .platform("ios")
                .appVersion("1.4.0")
                .osVersion("26.0")
                .supportsNativeAlarm(true)
                .nativeAlarmProvider("iosAlarmKit")
                .fallbackProvider("localNotification")
                .build();

        AlarmDeviceCurrentResponseDto response = alarmService.registerCurrentDevice(
                USER_ID,
                request,
                ACCESS_TOKEN,
                REFRESH_TOKEN
        );

        assertThat(response.getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(response.getActive()).isTrue();
        assertThat(oldDevice.getActive()).isFalse();
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    @DisplayName("reportAlarmStatus rejects a device whose session token is no longer current")
    void reportAlarmStatusRejectsWrongSession() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 11L);
        currentDevice.bindSession("different-access-token", null);

        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.of(currentDevice));

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, validStatusReport(), ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEVICE_SESSION_NOT_ACTIVE);
    }

    @Test
    @DisplayName("shouldSuppressLegacyReminder only suppresses fresh current-session armed schedules")
    void shouldSuppressLegacyReminderRequiresCurrentSessionAndArmedSchedule() {
        UUID scheduleId = UUID.fromString("123e4567-e89b-12d3-a456-426614170105");
        UserDevice currentDevice = userDevice(DEVICE_ID, 12L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        UserAlarmStatus alarmStatus = UserAlarmStatus.builder()
                .user(user)
                .userDevice(currentDevice)
                .deviceId(DEVICE_ID)
                .reconciledAt(Instant.now())
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 12, 0, 0))
                .status("armed")
                .nativeAlarmProvider("androidAlarmManager")
                .fallbackProvider("none")
                .armedScheduleCount(1)
                .armedScheduleIds("[\"" + scheduleId + "\"]")
                .skippedScheduleCount(0)
                .failures("[]")
                .updatedAt(Instant.now())
                .build();

        when(userAlarmSettingRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(UserAlarmSetting.defaultFor(user)));
        when(userDeviceRepository.findFirstByUserIdAndActiveTrueOrderByLastSeenAtDesc(USER_ID))
                .thenReturn(Optional.of(currentDevice));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAlarmStatusRepository.findByUserDeviceUserDeviceId(currentDevice.getUserDeviceId()))
                .thenReturn(Optional.of(alarmStatus));

        boolean shouldSuppress = alarmService.shouldSuppressLegacyReminder(
                USER_ID,
                scheduleId,
                LocalDateTime.of(2026, 5, 5, 8, 50)
        );

        assertThat(shouldSuppress).isTrue();
    }

    @Test
    @DisplayName("patchAlarmSettings rejects fractional defaultAlarmOffsetMinutes")
    void patchAlarmSettingsRejectsFractionalOffset() {
        assertThatThrownBy(() -> alarmService.patchAlarmSettings(
                USER_ID,
                Map.of("defaultAlarmOffsetMinutes", 5.5)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects unsupported when local notification fallback is active")
    void reportAlarmStatusRejectsUnsupportedWithFallbackCoverage() {
        assertThatThrownBy(() -> alarmService.reportAlarmStatus(
                USER_ID,
                unsupportedWithFallbackStatusReport(),
                ACCESS_TOKEN
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private AlarmStatusReportRequestDto validStatusReport() {
        return AlarmStatusReportRequestDto.builder()
                .deviceId(DEVICE_ID)
                .reconciledAt(OffsetDateTime.parse("2026-05-05T09:00:00.000Z"))
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 13, 0, 0))
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 12, 0, 0))
                .status("armed")
                .nativeAlarmProvider("iosAlarmKit")
                .fallbackProvider("localNotification")
                .armedScheduleCount(0)
                .armedScheduleIds(List.of())
                .skippedScheduleCount(0)
                .build();
    }

    private AlarmStatusReportRequestDto unsupportedWithFallbackStatusReport() {
        return AlarmStatusReportRequestDto.builder()
                .deviceId(DEVICE_ID)
                .reconciledAt(OffsetDateTime.parse("2026-05-05T09:00:00.000Z"))
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 13, 0, 0))
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 12, 0, 0))
                .status("unsupported")
                .nativeAlarmProvider("none")
                .fallbackProvider("localNotification")
                .armedScheduleCount(0)
                .armedScheduleIds(List.of())
                .skippedScheduleCount(0)
                .build();
    }

    private UserDevice userDevice(String deviceId, Long userDeviceId) {
        return UserDevice.builder()
                .userDeviceId(userDeviceId)
                .user(user)
                .deviceId(deviceId)
                .platform("ios")
                .supportsNativeAlarm(true)
                .nativeAlarmProvider("iosAlarmKit")
                .fallbackProvider("localNotification")
                .active(true)
                .lastSeenAt(Instant.now())
                .build();
    }
}
