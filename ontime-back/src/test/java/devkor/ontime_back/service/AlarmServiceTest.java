package devkor.ontime_back.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.AlarmDeviceCurrentRequestDto;
import devkor.ontime_back.dto.AlarmDeviceCurrentResponseDto;
import devkor.ontime_back.dto.AlarmDeviceUnregisterRequestDto;
import devkor.ontime_back.dto.AlarmSettingsResponseDto;
import devkor.ontime_back.dto.AlarmSettingsPatchDto;
import devkor.ontime_back.dto.AlarmStatusCurrentResponseDto;
import devkor.ontime_back.dto.AlarmStatusFailureDto;
import devkor.ontime_back.dto.AlarmStatusReportRequestDto;
import devkor.ontime_back.dto.AlarmStatusReportResponseDto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
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
    @DisplayName("getAlarmSettings creates default settings when the user does not have them yet")
    void getAlarmSettingsCreatesDefaultSettings() {
        when(userAlarmSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAlarmSettingRepository.save(any(UserAlarmSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AlarmSettingsResponseDto response = alarmService.getAlarmSettings(USER_ID);

        assertThat(response.getAlarmsEnabled()).isTrue();
        assertThat(response.getDefaultAlarmOffsetMinutes()).isEqualTo(UserAlarmSetting.DEFAULT_ALARM_OFFSET_MINUTES);
        verify(userAlarmSettingRepository).save(any(UserAlarmSetting.class));
    }

    @Test
    @DisplayName("getAlarmSettings fails when defaults cannot be created for a missing user")
    void getAlarmSettingsRejectsMissingUser() {
        when(userAlarmSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmService.getAlarmSettings(USER_ID))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("patchAlarmSettings updates only the provided fields")
    void patchAlarmSettingsUpdatesProvidedFields() {
        UserAlarmSetting setting = UserAlarmSetting.defaultFor(user);
        when(userAlarmSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(setting));

        AlarmSettingsResponseDto response = alarmService.patchAlarmSettings(
                USER_ID,
                Map.of("alarmsEnabled", false, "defaultAlarmOffsetMinutes", 30L)
        );

        assertThat(response.getAlarmsEnabled()).isFalse();
        assertThat(response.getDefaultAlarmOffsetMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("patchAlarmSettings accepts the validated DTO shape used by the controller")
    void patchAlarmSettingsDtoUpdatesProvidedFields() {
        UserAlarmSetting setting = UserAlarmSetting.defaultFor(user);
        when(userAlarmSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(setting));
        AlarmSettingsPatchDto request = new AlarmSettingsPatchDto();
        ReflectionTestUtils.setField(request, "alarmsEnabled", false);
        ReflectionTestUtils.setField(request, "defaultAlarmOffsetMinutes", BigInteger.valueOf(45));

        AlarmSettingsResponseDto response = alarmService.patchAlarmSettings(USER_ID, request);

        assertThat(response.getAlarmsEnabled()).isFalse();
        assertThat(response.getDefaultAlarmOffsetMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("patchAlarmSettings rejects an empty patch body")
    void patchAlarmSettingsRejectsEmptyPatchBody() {
        assertThatThrownBy(() -> alarmService.patchAlarmSettings(USER_ID, Map.of()))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("patchAlarmSettings rejects unknown fields to protect the API contract")
    void patchAlarmSettingsRejectsUnknownFields() {
        assertThatThrownBy(() -> alarmService.patchAlarmSettings(USER_ID, Map.of("token", "secret")))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALARM_SETTINGS_INVALID_FIELD);
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
    @DisplayName("reportAlarmStatus persists normalized counts and failure details for the active device session")
    void reportAlarmStatusPersistsStatusForCurrentDeviceSession() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 11L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        AlarmStatusReportRequestDto request = AlarmStatusReportRequestDto.builder()
                .deviceId(DEVICE_ID)
                .reconciledAt(OffsetDateTime.parse("2026-05-05T09:00:00.000Z"))
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 13, 0, 0))
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 12, 0, 0))
                .status("armed")
                .nativeAlarmProvider("iosAlarmKit")
                .fallbackProvider("localNotification")
                .armedScheduleCount(null)
                .armedScheduleIds(null)
                .skippedScheduleCount(null)
                .failures(List.of(AlarmStatusFailureDto.builder()
                        .scheduleId("schedule-1")
                        .reason("platformError")
                        .build()))
                .build();

        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.of(currentDevice));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAlarmStatusRepository.findByUserDeviceUserDeviceId(11L)).thenReturn(Optional.empty());

        AlarmStatusReportResponseDto response = alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN);

        assertThat(response.getReceived()).isTrue();
        ArgumentCaptor<UserAlarmStatus> captor = forClass(UserAlarmStatus.class);
        verify(userAlarmStatusRepository).save(captor.capture());
        UserAlarmStatus savedStatus = captor.getValue();
        assertThat(savedStatus.getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(savedStatus.getArmedScheduleCount()).isZero();
        assertThat(savedStatus.getArmedScheduleIds()).isEqualTo("[]");
        assertThat(savedStatus.getSkippedScheduleCount()).isZero();
        assertThat(savedStatus.getFailures()).contains("platformError");
    }

    @Test
    @DisplayName("reportAlarmStatus updates an existing status record for the active device")
    void reportAlarmStatusUpdatesExistingDeviceStatus() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 11L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        UserAlarmStatus existingStatus = UserAlarmStatus.create(user, currentDevice);
        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.of(currentDevice));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAlarmStatusRepository.findByUserDeviceUserDeviceId(11L)).thenReturn(Optional.of(existingStatus));

        alarmService.reportAlarmStatus(USER_ID, validStatusReport(), ACCESS_TOKEN);

        assertThat(existingStatus.getStatus()).isEqualTo("armed");
        verify(userAlarmStatusRepository).save(existingStatus);
    }

    @Test
    @DisplayName("getCurrentAlarmStatus returns inactive when no active device belongs to the access token")
    void getCurrentAlarmStatusReturnsInactiveWithoutCurrentSessionDevice() {
        UserDevice otherDevice = userDevice(DEVICE_ID, 12L);
        otherDevice.bindSession("other-token", null);
        when(userDeviceRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(otherDevice));

        AlarmStatusCurrentResponseDto response = alarmService.getCurrentAlarmStatus(USER_ID, ACCESS_TOKEN);

        assertThat(response.getActive()).isFalse();
    }

    @Test
    @DisplayName("getCurrentAlarmStatus returns the active device and parsed status payload")
    void getCurrentAlarmStatusReturnsDeviceAndLatestStatus() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 12L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        UserAlarmStatus status = UserAlarmStatus.builder()
                .user(user)
                .userDevice(currentDevice)
                .deviceId(DEVICE_ID)
                .reconciledAt(Instant.parse("2026-05-05T09:00:00Z"))
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 13, 0, 0))
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 12, 0, 0))
                .status("partial")
                .permissionIssue("nativePermissionDenied")
                .nativeAlarmProvider("none")
                .fallbackProvider("localNotification")
                .armedScheduleCount(1)
                .armedScheduleIds("[\"schedule-1\"]")
                .skippedScheduleCount(2)
                .failures("[{\"scheduleId\":\"schedule-2\",\"reason\":\"scheduleInvalid\"}]")
                .updatedAt(Instant.parse("2026-05-05T09:01:00Z"))
                .build();

        when(userDeviceRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(currentDevice));
        when(userAlarmStatusRepository.findByUserDeviceUserDeviceId(12L)).thenReturn(Optional.of(status));

        AlarmStatusCurrentResponseDto response = alarmService.getCurrentAlarmStatus(USER_ID, ACCESS_TOKEN);

        assertThat(response.getActive()).isTrue();
        assertThat(response.getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(response.getStatus()).isEqualTo("partial");
        assertThat(response.getArmedScheduleIds()).containsExactly("schedule-1");
        assertThat(response.getFailures()).extracting(AlarmStatusFailureDto::getReason).containsExactly("scheduleInvalid");
    }

    @Test
    @DisplayName("getCurrentAlarmStatus treats malformed stored JSON as empty lists")
    void getCurrentAlarmStatusIgnoresMalformedStoredJsonLists() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 12L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        UserAlarmStatus status = UserAlarmStatus.builder()
                .user(user)
                .userDevice(currentDevice)
                .deviceId(DEVICE_ID)
                .reconciledAt(Instant.parse("2026-05-05T09:00:00Z"))
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 13, 0, 0))
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 12, 0, 0))
                .status("partial")
                .nativeAlarmProvider("none")
                .fallbackProvider("localNotification")
                .armedScheduleCount(1)
                .armedScheduleIds("not-json")
                .skippedScheduleCount(2)
                .failures("not-json")
                .updatedAt(Instant.parse("2026-05-05T09:01:00Z"))
                .build();
        when(userDeviceRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(currentDevice));
        when(userAlarmStatusRepository.findByUserDeviceUserDeviceId(12L)).thenReturn(Optional.of(status));

        AlarmStatusCurrentResponseDto response = alarmService.getCurrentAlarmStatus(USER_ID, ACCESS_TOKEN);

        assertThat(response.getArmedScheduleIds()).isEmpty();
        assertThat(response.getFailures()).isEmpty();
    }

    @Test
    @DisplayName("unregisterCurrentDevice deactivates the named current-session device")
    void unregisterCurrentDeviceDeactivatesNamedSessionDevice() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 13L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.of(currentDevice));

        alarmService.unregisterCurrentDevice(
                USER_ID,
                AlarmDeviceUnregisterRequestDto.builder().deviceId(DEVICE_ID).build(),
                ACCESS_TOKEN
        );

        assertThat(currentDevice.getActive()).isFalse();
    }

    @Test
    @DisplayName("unregisterCurrentDevice rejects a named device that is not active")
    void unregisterCurrentDeviceRejectsMissingNamedDevice() {
        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmService.unregisterCurrentDevice(
                USER_ID,
                AlarmDeviceUnregisterRequestDto.builder().deviceId(DEVICE_ID).build(),
                ACCESS_TOKEN
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEVICE_SESSION_NOT_ACTIVE);
    }

    @Test
    @DisplayName("unregisterCurrentDevice deactivates only devices bound to the current session when present")
    void unregisterCurrentDeviceWithoutDeviceIdPrefersCurrentSessionDevices() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 13L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        UserDevice otherDevice = userDevice("other-device-id-1234", 14L);
        otherDevice.bindSession("other-token", null);
        when(userDeviceRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(currentDevice, otherDevice));

        alarmService.unregisterCurrentDevice(USER_ID, null, ACCESS_TOKEN);

        assertThat(currentDevice.getActive()).isFalse();
        assertThat(otherDevice.getActive()).isTrue();
    }

    @Test
    @DisplayName("unregisterCurrentDevice deactivates all active devices when none belongs to the current session")
    void unregisterCurrentDeviceWithoutDeviceIdDeactivatesAllDevicesWhenNoSessionDeviceMatches() {
        UserDevice firstDevice = userDevice(DEVICE_ID, 13L);
        firstDevice.bindSession("first-token", null);
        UserDevice secondDevice = userDevice("other-device-id-1234", 14L);
        secondDevice.bindSession("second-token", null);
        when(userDeviceRepository.findAllByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(firstDevice, secondDevice));

        alarmService.unregisterCurrentDevice(USER_ID, null, ACCESS_TOKEN);

        assertThat(firstDevice.getActive()).isFalse();
        assertThat(secondDevice.getActive()).isFalse();
    }

    @Test
    @DisplayName("linkFirebaseToken writes the token only for the active device session")
    void linkFirebaseTokenUpdatesCurrentSessionDevice() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 15L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.of(currentDevice));

        alarmService.linkFirebaseToken(USER_ID, DEVICE_ID, "firebase-token", ACCESS_TOKEN);

        assertThat(currentDevice.getFirebaseToken()).isEqualTo("firebase-token");
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
    @DisplayName("shouldSuppressLegacyReminder does not trust stale native alarm reconciliation")
    void shouldSuppressLegacyReminderReturnsFalseForStaleAlarmStatus() {
        UUID scheduleId = UUID.fromString("123e4567-e89b-12d3-a456-426614170105");
        UserDevice currentDevice = userDevice(DEVICE_ID, 12L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        UserAlarmStatus alarmStatus = UserAlarmStatus.builder()
                .user(user)
                .userDevice(currentDevice)
                .deviceId(DEVICE_ID)
                .reconciledAt(Instant.now().minus(Duration.ofHours(25)))
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

        assertThat(shouldSuppress).isFalse();
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
    @DisplayName("patchAlarmSettings accepts small integer JSON numeric types")
    void patchAlarmSettingsAcceptsShortAndByteOffsets() {
        UserAlarmSetting setting = UserAlarmSetting.defaultFor(user);
        when(userAlarmSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(setting));

        AlarmSettingsResponseDto shortResponse = alarmService.patchAlarmSettings(
                USER_ID,
                Map.of("defaultAlarmOffsetMinutes", Short.valueOf((short) 12))
        );
        AlarmSettingsResponseDto byteResponse = alarmService.patchAlarmSettings(
                USER_ID,
                Map.of("defaultAlarmOffsetMinutes", Byte.valueOf((byte) 5))
        );

        assertThat(shortResponse.getDefaultAlarmOffsetMinutes()).isEqualTo(12);
        assertThat(byteResponse.getDefaultAlarmOffsetMinutes()).isEqualTo(5);
    }

    @Test
    @DisplayName("patchAlarmSettings rejects integer values outside supported API range")
    void patchAlarmSettingsRejectsOutOfRangeNumericTypes() {
        assertThatThrownBy(() -> alarmService.patchAlarmSettings(
                USER_ID,
                Map.of("defaultAlarmOffsetMinutes", BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE))
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("registerCurrentDevice rejects provider combinations that cannot run on the platform")
    void registerCurrentDeviceRejectsWrongPlatformProvider() {
        AlarmDeviceCurrentRequestDto request = AlarmDeviceCurrentRequestDto.builder()
                .deviceId(DEVICE_ID)
                .platform("ios")
                .supportsNativeAlarm(true)
                .nativeAlarmProvider("androidAlarmManager")
                .fallbackProvider("localNotification")
                .build();

        assertThatThrownBy(() -> alarmService.registerCurrentDevice(USER_ID, request, ACCESS_TOKEN, REFRESH_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("registerCurrentDevice rejects missing request bodies")
    void registerCurrentDeviceRejectsMissingRequestBody() {
        assertThatThrownBy(() -> alarmService.registerCurrentDevice(USER_ID, null, ACCESS_TOKEN, REFRESH_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("registerCurrentDevice rejects blank access tokens")
    void registerCurrentDeviceRejectsBlankAccessToken() {
        assertThatThrownBy(() -> alarmService.registerCurrentDevice(
                USER_ID,
                validDeviceRegistration().build(),
                " ",
                REFRESH_TOKEN
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("registerCurrentDevice rejects app metadata that cannot fit the API contract")
    void registerCurrentDeviceRejectsOverlongVersionMetadata() {
        AlarmDeviceCurrentRequestDto request = validDeviceRegistration()
                .appVersion("v".repeat(129))
                .build();

        assertThatThrownBy(() -> alarmService.registerCurrentDevice(USER_ID, request, ACCESS_TOKEN, REFRESH_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("registerCurrentDevice rejects native providers when the device says native alarms are unsupported")
    void registerCurrentDeviceRejectsNativeProviderWithoutNativeSupport() {
        AlarmDeviceCurrentRequestDto request = validDeviceRegistration()
                .supportsNativeAlarm(false)
                .nativeAlarmProvider("iosAlarmKit")
                .build();

        assertThatThrownBy(() -> alarmService.registerCurrentDevice(USER_ID, request, ACCESS_TOKEN, REFRESH_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("registerCurrentDevice fails when the current user no longer exists")
    void registerCurrentDeviceRejectsMissingUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmService.registerCurrentDevice(
                USER_ID,
                validDeviceRegistration().build(),
                ACCESS_TOKEN,
                REFRESH_TOKEN
        ))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
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

    @Test
    @DisplayName("reportAlarmStatus rejects inverted schedule windows")
    void reportAlarmStatusRejectsInvalidScheduleWindow() {
        AlarmStatusReportRequestDto request = validStatusReportBuilder()
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 6, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 5, 0, 0))
                .build();

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects unknown permission issues")
    void reportAlarmStatusRejectsInvalidPermissionIssue() {
        AlarmStatusReportRequestDto request = validStatusReportBuilder()
                .permissionIssue("cameraPermissionDenied")
                .build();

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects unknown failure reasons")
    void reportAlarmStatusRejectsInvalidFailureReason() {
        AlarmStatusReportRequestDto request = validStatusReportBuilder()
                .failures(List.of(AlarmStatusFailureDto.builder()
                        .scheduleId("schedule-1")
                        .reason("networkUnavailable")
                        .build()))
                .build();

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects negative alarm counts")
    void reportAlarmStatusRejectsNegativeCounts() {
        AlarmStatusReportRequestDto request = validStatusReportBuilder()
                .armedScheduleCount(-1)
                .build();

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects negative skipped counts")
    void reportAlarmStatusRejectsNegativeSkippedCounts() {
        AlarmStatusReportRequestDto request = validStatusReportBuilder()
                .skippedScheduleCount(-1)
                .build();

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects inverted alarm coverage windows")
    void reportAlarmStatusRejectsInvalidCoverageWindow() {
        AlarmStatusReportRequestDto request = validStatusReportBuilder()
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 6, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 5, 0, 0))
                .build();

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects missing required status payloads")
    void reportAlarmStatusRejectsMissingStatusPayload() {
        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, null, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus rejects null failure entries")
    void reportAlarmStatusRejectsNullFailureEntry() {
        AlarmStatusReportRequestDto request = validStatusReportBuilder()
                .failures(java.util.Arrays.asList((AlarmStatusFailureDto) null))
                .build();

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, request, ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("reportAlarmStatus fails when the owning user disappears before status persistence")
    void reportAlarmStatusRejectsMissingUserAfterSessionValidation() {
        UserDevice currentDevice = userDevice(DEVICE_ID, 11L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.of(currentDevice));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmService.reportAlarmStatus(USER_ID, validStatusReport(), ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("linkFirebaseToken rejects missing active device sessions")
    void linkFirebaseTokenRejectsMissingCurrentDevice() {
        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(USER_ID, DEVICE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> alarmService.linkFirebaseToken(USER_ID, DEVICE_ID, "firebase-token", ACCESS_TOKEN))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEVICE_SESSION_NOT_ACTIVE);
    }

    @Test
    @DisplayName("shouldSuppressLegacyReminder does not suppress when user disabled native alarms")
    void shouldSuppressLegacyReminderReturnsFalseWhenAlarmsDisabled() {
        UserAlarmSetting setting = UserAlarmSetting.defaultFor(user);
        setting.update(false, null);
        when(userAlarmSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(setting));

        boolean shouldSuppress = alarmService.shouldSuppressLegacyReminder(
                USER_ID,
                UUID.randomUUID(),
                LocalDateTime.of(2026, 5, 5, 8, 50)
        );

        assertThat(shouldSuppress).isFalse();
    }

    @Test
    @DisplayName("shouldSuppressLegacyReminder does not suppress partial reports with no armed schedules")
    void shouldSuppressLegacyReminderReturnsFalseForPartialStatusWithoutArmedSchedules() {
        UUID scheduleId = UUID.fromString("123e4567-e89b-12d3-a456-426614170105");
        UserAlarmStatus status = suppressibleStatus(scheduleId);
        status.replace(
                Instant.now(),
                status.getScheduleWindowStart(),
                status.getScheduleWindowEnd(),
                status.getAlarmCoverageStart(),
                status.getAlarmCoverageEnd(),
                "partial",
                null,
                "iosAlarmKit",
                "localNotification",
                0,
                "[\"" + scheduleId + "\"]",
                0,
                "[]"
        );
        stubSuppressionContext(status);

        boolean shouldSuppress = alarmService.shouldSuppressLegacyReminder(
                USER_ID,
                scheduleId,
                LocalDateTime.of(2026, 5, 5, 8, 50)
        );

        assertThat(shouldSuppress).isFalse();
    }

    @Test
    @DisplayName("shouldSuppressLegacyReminder requires the reminder time to be inside native coverage")
    void shouldSuppressLegacyReminderReturnsFalseOutsideCoverageWindow() {
        UUID scheduleId = UUID.fromString("123e4567-e89b-12d3-a456-426614170105");
        stubSuppressionContext(suppressibleStatus(scheduleId));

        boolean shouldSuppress = alarmService.shouldSuppressLegacyReminder(
                USER_ID,
                scheduleId,
                LocalDateTime.of(2026, 5, 12, 0, 0)
        );

        assertThat(shouldSuppress).isFalse();
    }

    @Test
    @DisplayName("shouldSuppressLegacyReminder requires the schedule id to be in the armed schedule list")
    void shouldSuppressLegacyReminderReturnsFalseWhenScheduleWasNotArmed() {
        UUID scheduleId = UUID.fromString("123e4567-e89b-12d3-a456-426614170105");
        UUID otherScheduleId = UUID.fromString("123e4567-e89b-12d3-a456-426614170106");
        stubSuppressionContext(suppressibleStatus(otherScheduleId));

        boolean shouldSuppress = alarmService.shouldSuppressLegacyReminder(
                USER_ID,
                scheduleId,
                LocalDateTime.of(2026, 5, 5, 8, 50)
        );

        assertThat(shouldSuppress).isFalse();
    }

    @Test
    @DisplayName("shouldSuppressLegacyReminder does not suppress when no provider covers the schedule")
    void shouldSuppressLegacyReminderReturnsFalseWithoutProviderCoverage() {
        UUID scheduleId = UUID.fromString("123e4567-e89b-12d3-a456-426614170105");
        UserAlarmStatus status = suppressibleStatus(scheduleId);
        status.replace(
                Instant.now(),
                status.getScheduleWindowStart(),
                status.getScheduleWindowEnd(),
                status.getAlarmCoverageStart(),
                status.getAlarmCoverageEnd(),
                "armed",
                null,
                "none",
                "none",
                1,
                "[\"" + scheduleId + "\"]",
                0,
                "[]"
        );
        stubSuppressionContext(status);

        assertThat(alarmService.shouldSuppressLegacyReminder(
                USER_ID,
                scheduleId,
                LocalDateTime.of(2026, 5, 5, 8, 50)
        )).isFalse();
    }

    private AlarmStatusReportRequestDto validStatusReport() {
        return validStatusReportBuilder().build();
    }

    private AlarmStatusReportRequestDto.AlarmStatusReportRequestDtoBuilder validStatusReportBuilder() {
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
                .skippedScheduleCount(0);
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

    private AlarmDeviceCurrentRequestDto.AlarmDeviceCurrentRequestDtoBuilder validDeviceRegistration() {
        return AlarmDeviceCurrentRequestDto.builder()
                .deviceId(DEVICE_ID)
                .platform("ios")
                .appVersion("1.4.0")
                .osVersion("26.0")
                .supportsNativeAlarm(true)
                .nativeAlarmProvider("iosAlarmKit")
                .fallbackProvider("localNotification");
    }

    private void stubSuppressionContext(UserAlarmStatus status) {
        UserDevice currentDevice = status.getUserDevice();
        currentDevice.bindSession(ACCESS_TOKEN, null);
        when(userAlarmSettingRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(UserAlarmSetting.defaultFor(user)));
        when(userDeviceRepository.findFirstByUserIdAndActiveTrueOrderByLastSeenAtDesc(USER_ID))
                .thenReturn(Optional.of(currentDevice));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAlarmStatusRepository.findByUserDeviceUserDeviceId(currentDevice.getUserDeviceId()))
                .thenReturn(Optional.of(status));
    }

    private UserAlarmStatus suppressibleStatus(UUID scheduleId) {
        UserDevice currentDevice = userDevice(DEVICE_ID, 12L);
        currentDevice.bindSession(ACCESS_TOKEN, null);
        return UserAlarmStatus.builder()
                .user(user)
                .userDevice(currentDevice)
                .deviceId(DEVICE_ID)
                .reconciledAt(Instant.now())
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 13, 0, 0))
                .alarmCoverageStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .alarmCoverageEnd(LocalDateTime.of(2026, 5, 12, 0, 0))
                .status("armed")
                .nativeAlarmProvider("iosAlarmKit")
                .fallbackProvider("localNotification")
                .armedScheduleCount(1)
                .armedScheduleIds("[\"" + scheduleId + "\"]")
                .skippedScheduleCount(0)
                .failures("[]")
                .updatedAt(Instant.now())
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
