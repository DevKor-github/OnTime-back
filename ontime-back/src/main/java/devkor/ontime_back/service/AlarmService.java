package devkor.ontime_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.dto.*;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAlarmSetting;
import devkor.ontime_back.entity.UserAlarmStatus;
import devkor.ontime_back.entity.UserDevice;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserAlarmStatusRepository;
import devkor.ontime_back.repository.UserDeviceRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static devkor.ontime_back.response.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {

    private static final int MAX_DEFAULT_ALARM_OFFSET_MINUTES = 1440;
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{16,128}$");
    private static final Set<String> ALARM_SETTING_FIELDS = Set.of("alarmsEnabled", "defaultAlarmOffsetMinutes");
    private static final Set<String> PLATFORMS = Set.of("android", "ios");
    private static final Set<String> NATIVE_PROVIDERS = Set.of("androidAlarmManager", "iosAlarmKit", "none");
    private static final Set<String> FALLBACK_PROVIDERS = Set.of("localNotification", "none");
    private static final Set<String> STATUSES = Set.of("armed", "partial", "disabled", "permissionNeeded", "unsupported", "settingsUnavailable");
    private static final Set<String> PERMISSION_ISSUES = Set.of("nativePermissionDenied", "notificationPermissionDenied");
    private static final Set<String> FAILURE_REASONS = Set.of("preparationLoadFailed", "scheduleInvalid", "platformError", "unknown");
    private static final String NATIVE_NONE = "none";
    private static final String FALLBACK_LOCAL_NOTIFICATION = "localNotification";

    private final UserRepository userRepository;
    private final UserAlarmSettingRepository userAlarmSettingRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserAlarmStatusRepository userAlarmStatusRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AlarmSettingsResponseDto getAlarmSettings(Long userId) {
        UserAlarmSetting setting = getOrCreateAlarmSetting(userId);
        return toAlarmSettingsResponse(setting);
    }

    @Transactional
    public AlarmSettingsResponseDto patchAlarmSettings(Long userId, Map<String, Object> requestBody) {
        if (requestBody == null || requestBody.isEmpty()) {
            throw new GeneralException(INVALID_INPUT);
        }

        for (String field : requestBody.keySet()) {
            if (!ALARM_SETTING_FIELDS.contains(field)) {
                throw new GeneralException(ALARM_SETTINGS_INVALID_FIELD);
            }
        }

        Boolean alarmsEnabled = null;
        Integer defaultAlarmOffsetMinutes = null;

        if (requestBody.containsKey("alarmsEnabled")) {
            Object value = requestBody.get("alarmsEnabled");
            if (!(value instanceof Boolean)) {
                throw new GeneralException(INVALID_INPUT);
            }
            alarmsEnabled = (Boolean) value;
        }

        if (requestBody.containsKey("defaultAlarmOffsetMinutes")) {
            Object value = requestBody.get("defaultAlarmOffsetMinutes");
            defaultAlarmOffsetMinutes = parseInteger(value);
            if (defaultAlarmOffsetMinutes < 0 || defaultAlarmOffsetMinutes > MAX_DEFAULT_ALARM_OFFSET_MINUTES) {
                throw new GeneralException(INVALID_INPUT);
            }
        }

        UserAlarmSetting setting = getOrCreateAlarmSetting(userId);
        setting.update(alarmsEnabled, defaultAlarmOffsetMinutes);
        return toAlarmSettingsResponse(setting);
    }

    @Transactional
    public AlarmDeviceCurrentResponseDto registerCurrentDevice(Long userId,
                                                               AlarmDeviceCurrentRequestDto requestDto,
                                                               String accessToken,
                                                               String refreshToken) {
        validateDeviceRegistration(requestDto);
        validateSessionAccessToken(accessToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
        Instant now = Instant.now();

        List<UserDevice> activeDevices = userDeviceRepository.findAllByUserIdAndActiveTrue(userId);
        for (UserDevice activeDevice : activeDevices) {
            activeDevice.deactivate();
        }

        UserDevice device = userDeviceRepository.findByUserIdAndDeviceId(userId, requestDto.getDeviceId())
                .orElseGet(() -> UserDevice.create(user, requestDto.getDeviceId()));
        device.activate(
                requestDto.getPlatform(),
                requestDto.getAppVersion(),
                requestDto.getOsVersion(),
                Boolean.TRUE.equals(requestDto.getSupportsNativeAlarm()),
                requestDto.getNativeAlarmProvider(),
                requestDto.getFallbackProvider(),
                now
        );
        device.bindSession(accessToken, refreshToken);

        UserDevice savedDevice = userDeviceRepository.save(device);
        return AlarmDeviceCurrentResponseDto.builder()
                .deviceId(savedDevice.getDeviceId())
                .active(savedDevice.getActive())
                .lastSeenAt(savedDevice.getLastSeenAt())
                .build();
    }

    @Transactional
    public AlarmDeviceUnregisterResponseDto unregisterCurrentDevice(Long userId,
                                                                    AlarmDeviceUnregisterRequestDto requestDto,
                                                                    String accessToken) {
        validateSessionAccessToken(accessToken);
        if (requestDto != null && requestDto.getDeviceId() != null && !requestDto.getDeviceId().isBlank()) {
            validateDeviceId(requestDto.getDeviceId());
            UserDevice currentDevice = userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(userId, requestDto.getDeviceId())
                    .orElseThrow(() -> new GeneralException(DEVICE_SESSION_NOT_ACTIVE));
            ensureDeviceSessionActive(currentDevice, accessToken);
            currentDevice.deactivate();
        } else {
            List<UserDevice> activeDevices = userDeviceRepository.findAllByUserIdAndActiveTrue(userId);
            List<UserDevice> currentSessionDevices = activeDevices.stream()
                    .filter(device -> device.belongsToAccessToken(accessToken))
                    .toList();
            if (currentSessionDevices.isEmpty()) {
                activeDevices.forEach(UserDevice::deactivate);
            } else {
                currentSessionDevices.forEach(UserDevice::deactivate);
            }
        }

        return AlarmDeviceUnregisterResponseDto.builder()
                .active(false)
                .build();
    }

    @Transactional
    public AlarmStatusReportResponseDto reportAlarmStatus(Long userId,
                                                          AlarmStatusReportRequestDto requestDto,
                                                          String accessToken) {
        validateAlarmStatusReport(requestDto);
        validateSessionAccessToken(accessToken);

        UserDevice currentDevice = userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(userId, requestDto.getDeviceId())
                .orElseThrow(() -> new GeneralException(DEVICE_SESSION_NOT_ACTIVE));
        ensureDeviceSessionActive(currentDevice, accessToken);
        validateProviderCompatibility(
                currentDevice.getPlatform(),
                requestDto.getNativeAlarmProvider(),
                Boolean.TRUE.equals(currentDevice.getSupportsNativeAlarm())
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(USER_NOT_FOUND));

        UserAlarmStatus alarmStatus = userAlarmStatusRepository.findByUserDeviceUserDeviceId(currentDevice.getUserDeviceId())
                .orElseGet(() -> UserAlarmStatus.create(user, currentDevice));

        alarmStatus.replace(
                requestDto.getReconciledAt().toInstant(),
                requestDto.getScheduleWindowStart(),
                requestDto.getScheduleWindowEnd(),
                requestDto.getAlarmCoverageStart(),
                requestDto.getAlarmCoverageEnd(),
                requestDto.getStatus(),
                requestDto.getPermissionIssue(),
                requestDto.getNativeAlarmProvider(),
                requestDto.getFallbackProvider(),
                defaultNonNegative(requestDto.getArmedScheduleCount()),
                toJson(defaultList(requestDto.getArmedScheduleIds())),
                defaultNonNegative(requestDto.getSkippedScheduleCount()),
                toJson(defaultList(requestDto.getFailures()))
        );

        userAlarmStatusRepository.save(alarmStatus);
        return AlarmStatusReportResponseDto.builder()
                .received(true)
                .build();
    }

    public AlarmStatusCurrentResponseDto getCurrentAlarmStatus(Long userId, String accessToken) {
        validateSessionAccessToken(accessToken);

        Optional<UserDevice> currentDevice = userDeviceRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .filter(device -> device.belongsToAccessToken(accessToken))
                .findFirst();
        if (currentDevice.isEmpty()) {
            return AlarmStatusCurrentResponseDto.builder()
                    .active(false)
                    .build();
        }

        UserDevice device = currentDevice.get();
        Optional<UserAlarmStatus> latestStatus = userAlarmStatusRepository.findByUserDeviceUserDeviceId(device.getUserDeviceId());

        AlarmStatusCurrentResponseDto.AlarmStatusCurrentResponseDtoBuilder builder = AlarmStatusCurrentResponseDto.builder()
                .deviceId(device.getDeviceId())
                .active(device.getActive())
                .platform(device.getPlatform())
                .appVersion(device.getAppVersion())
                .osVersion(device.getOsVersion())
                .supportsNativeAlarm(device.getSupportsNativeAlarm())
                .nativeAlarmProvider(device.getNativeAlarmProvider())
                .fallbackProvider(device.getFallbackProvider())
                .lastSeenAt(device.getLastSeenAt());

        latestStatus.ifPresent(status -> builder
                .reconciledAt(status.getReconciledAt())
                .scheduleWindowStart(status.getScheduleWindowStart())
                .scheduleWindowEnd(status.getScheduleWindowEnd())
                .alarmCoverageStart(status.getAlarmCoverageStart())
                .alarmCoverageEnd(status.getAlarmCoverageEnd())
                .status(status.getStatus())
                .permissionIssue(status.getPermissionIssue())
                .armedScheduleCount(status.getArmedScheduleCount())
                .armedScheduleIds(parseStringList(status.getArmedScheduleIds()))
                .skippedScheduleCount(status.getSkippedScheduleCount())
                .failures(parseFailureList(status.getFailures()))
                .updatedAt(status.getUpdatedAt()));

        return builder.build();
    }

    @Transactional
    public void linkFirebaseToken(Long userId, String deviceId, String firebaseToken, String accessToken) {
        validateDeviceId(deviceId);
        validateSessionAccessToken(accessToken);
        UserDevice currentDevice = userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(userId, deviceId)
                .orElseThrow(() -> new GeneralException(DEVICE_SESSION_NOT_ACTIVE));
        ensureDeviceSessionActive(currentDevice, accessToken);
        currentDevice.updateFirebaseToken(firebaseToken);
    }

    public Integer getDefaultAlarmOffsetMinutes(Long userId) {
        return userAlarmSettingRepository.findByUserId(userId)
                .map(UserAlarmSetting::getDefaultAlarmOffsetMinutes)
                .orElse(UserAlarmSetting.DEFAULT_ALARM_OFFSET_MINUTES);
    }

    public boolean shouldSuppressLegacyReminder(Long userId, UUID scheduleId, LocalDateTime reminderTime) {
        boolean alarmsEnabled = userAlarmSettingRepository.findByUserId(userId)
                .map(UserAlarmSetting::getAlarmsEnabled)
                .orElse(true);
        if (!alarmsEnabled) {
            return false;
        }

        Optional<UserDevice> currentDevice = userDeviceRepository.findFirstByUserIdAndActiveTrueOrderByLastSeenAtDesc(userId);
        if (currentDevice.isEmpty()) {
            return false;
        }
        String currentAccessToken = userRepository.findById(userId)
                .map(User::getAccessToken)
                .orElse(null);
        if (!currentDevice.get().belongsToAccessToken(currentAccessToken)) {
            return false;
        }

        Optional<UserAlarmStatus> latestStatus = userAlarmStatusRepository.findByUserDeviceUserDeviceId(currentDevice.get().getUserDeviceId());
        if (latestStatus.isEmpty()) {
            return false;
        }

        UserAlarmStatus status = latestStatus.get();
        if (status.getReconciledAt() == null || status.getReconciledAt().isBefore(Instant.now().minus(Duration.ofHours(24)))) {
            return false;
        }

        if (!hasProviderCoverage(status)) {
            return false;
        }

        if (!isSuppressibleStatus(status)) {
            return false;
        }

        if (reminderTime == null || status.getAlarmCoverageStart() == null || status.getAlarmCoverageEnd() == null) {
            return false;
        }

        if (reminderTime.isBefore(status.getAlarmCoverageStart()) || !reminderTime.isBefore(status.getAlarmCoverageEnd())) {
            return false;
        }

        return parseStringList(status.getArmedScheduleIds()).contains(scheduleId.toString());
    }

    private UserAlarmSetting getOrCreateAlarmSetting(Long userId) {
        return userAlarmSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
                    return userAlarmSettingRepository.save(UserAlarmSetting.defaultFor(user));
                });
    }

    private AlarmSettingsResponseDto toAlarmSettingsResponse(UserAlarmSetting setting) {
        return AlarmSettingsResponseDto.builder()
                .alarmsEnabled(setting.getAlarmsEnabled())
                .defaultAlarmOffsetMinutes(setting.getDefaultAlarmOffsetMinutes())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    private void validateDeviceRegistration(AlarmDeviceCurrentRequestDto requestDto) {
        if (requestDto == null) {
            throw new GeneralException(INVALID_INPUT);
        }
        validateDeviceId(requestDto.getDeviceId());
        validatePlatformAndProviders(
                requestDto.getPlatform(),
                requestDto.getNativeAlarmProvider(),
                requestDto.getFallbackProvider(),
                Boolean.TRUE.equals(requestDto.getSupportsNativeAlarm())
        );
        validateOptionalBoundedString(requestDto.getAppVersion());
        validateOptionalBoundedString(requestDto.getOsVersion());
    }

    private void validateAlarmStatusReport(AlarmStatusReportRequestDto requestDto) {
        if (requestDto == null) {
            throw new GeneralException(INVALID_INPUT);
        }
        validateDeviceId(requestDto.getDeviceId());

        if (requestDto.getReconciledAt() == null
                || requestDto.getScheduleWindowStart() == null
                || requestDto.getScheduleWindowEnd() == null
                || requestDto.getAlarmCoverageStart() == null
                || requestDto.getAlarmCoverageEnd() == null
                || requestDto.getStatus() == null
                || !STATUSES.contains(requestDto.getStatus())
                || requestDto.getNativeAlarmProvider() == null
                || !NATIVE_PROVIDERS.contains(requestDto.getNativeAlarmProvider())
                || requestDto.getFallbackProvider() == null
                || !FALLBACK_PROVIDERS.contains(requestDto.getFallbackProvider())) {
            throw new GeneralException(INVALID_INPUT);
        }

        if (requestDto.getPermissionIssue() != null && !PERMISSION_ISSUES.contains(requestDto.getPermissionIssue())) {
            throw new GeneralException(INVALID_INPUT);
        }

        if ("unsupported".equals(requestDto.getStatus())
                && (!NATIVE_NONE.equals(requestDto.getNativeAlarmProvider())
                || FALLBACK_LOCAL_NOTIFICATION.equals(requestDto.getFallbackProvider()))) {
            throw new GeneralException(INVALID_INPUT);
        }

        if (requestDto.getScheduleWindowEnd().isBefore(requestDto.getScheduleWindowStart())
                || requestDto.getAlarmCoverageEnd().isBefore(requestDto.getAlarmCoverageStart())) {
            throw new GeneralException(INVALID_INPUT);
        }

        if (requestDto.getArmedScheduleCount() != null && requestDto.getArmedScheduleCount() < 0) {
            throw new GeneralException(INVALID_INPUT);
        }
        if (requestDto.getSkippedScheduleCount() != null && requestDto.getSkippedScheduleCount() < 0) {
            throw new GeneralException(INVALID_INPUT);
        }

        for (AlarmStatusFailureDto failure : defaultList(requestDto.getFailures())) {
            if (failure == null || failure.getReason() == null || !FAILURE_REASONS.contains(failure.getReason())) {
                throw new GeneralException(INVALID_INPUT);
            }
        }
    }

    private void validateDeviceId(String deviceId) {
        if (deviceId == null || !DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
            throw new GeneralException(INVALID_INPUT);
        }
    }

    private void validateSessionAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GeneralException(INVALID_INPUT);
        }
    }

    private void ensureDeviceSessionActive(UserDevice device, String accessToken) {
        if (!device.belongsToAccessToken(accessToken)) {
            throw new GeneralException(DEVICE_SESSION_NOT_ACTIVE);
        }
    }

    private void validatePlatformAndProviders(String platform, String nativeAlarmProvider, String fallbackProvider, boolean supportsNativeAlarm) {
        if (platform == null || !PLATFORMS.contains(platform)
                || nativeAlarmProvider == null || !NATIVE_PROVIDERS.contains(nativeAlarmProvider)
                || fallbackProvider == null || !FALLBACK_PROVIDERS.contains(fallbackProvider)) {
            throw new GeneralException(INVALID_INPUT);
        }

        validateProviderCompatibility(platform, nativeAlarmProvider, supportsNativeAlarm);
    }

    private void validateProviderCompatibility(String platform, String nativeAlarmProvider, boolean supportsNativeAlarm) {
        if ("ios".equals(platform) && "androidAlarmManager".equals(nativeAlarmProvider)) {
            throw new GeneralException(INVALID_INPUT);
        }
        if ("android".equals(platform) && "iosAlarmKit".equals(nativeAlarmProvider)) {
            throw new GeneralException(INVALID_INPUT);
        }
        if (!supportsNativeAlarm && !NATIVE_NONE.equals(nativeAlarmProvider)) {
            throw new GeneralException(INVALID_INPUT);
        }
    }

    private void validateOptionalBoundedString(String value) {
        if (value != null && value.length() > 128) {
            throw new GeneralException(INVALID_INPUT);
        }
    }

    private int defaultNonNegative(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Long longValue && longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) {
            return longValue.intValue();
        }
        if (value instanceof Short shortValue) {
            return shortValue.intValue();
        }
        if (value instanceof Byte byteValue) {
            return byteValue.intValue();
        }
        if (value instanceof BigInteger bigIntegerValue
                && bigIntegerValue.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                && bigIntegerValue.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0) {
            return bigIntegerValue.intValue();
        }
        throw new GeneralException(INVALID_INPUT);
    }

    private <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new GeneralException(INVALID_INPUT);
        }
    }

    private boolean hasProviderCoverage(UserAlarmStatus status) {
        return !NATIVE_NONE.equals(status.getNativeAlarmProvider())
                || FALLBACK_LOCAL_NOTIFICATION.equals(status.getFallbackProvider());
    }

    private boolean isSuppressibleStatus(UserAlarmStatus status) {
        if ("armed".equals(status.getStatus())) {
            return true;
        }
        return "partial".equals(status.getStatus()) && status.getArmedScheduleCount() != null && status.getArmedScheduleCount() > 0;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<AlarmStatusFailureDto> parseFailureList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
