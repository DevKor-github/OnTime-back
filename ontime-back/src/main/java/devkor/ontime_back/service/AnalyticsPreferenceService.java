package devkor.ontime_back.service;

import devkor.ontime_back.dto.AnalyticsPreferenceResponseDto;
import devkor.ontime_back.dto.AnalyticsPreferenceUpdateDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAnalyticsPreference;
import devkor.ontime_back.repository.UserAnalyticsPreferenceRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.GeneralException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.ontime_back.response.ErrorCode.USER_NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class AnalyticsPreferenceService {

    private final UserRepository userRepository;
    private final UserAnalyticsPreferenceRepository userAnalyticsPreferenceRepository;
    private final boolean defaultEnabled;

    public AnalyticsPreferenceService(
            UserRepository userRepository,
            UserAnalyticsPreferenceRepository userAnalyticsPreferenceRepository,
            @Value("${analytics.preference.default-enabled:false}") boolean defaultEnabled) {
        this.userRepository = userRepository;
        this.userAnalyticsPreferenceRepository = userAnalyticsPreferenceRepository;
        this.defaultEnabled = defaultEnabled;
    }

    @Transactional
    public AnalyticsPreferenceResponseDto getAnalyticsPreference(Long userId) {
        UserAnalyticsPreference preference = getOrCreatePreference(userId);
        preference.alignToDefault(defaultEnabled);
        return toResponse(preference);
    }

    @Transactional
    public AnalyticsPreferenceResponseDto updateAnalyticsPreference(Long userId, AnalyticsPreferenceUpdateDto requestDto) {
        UserAnalyticsPreference preference = getOrCreatePreference(userId);
        preference.update(requestDto.getEnabledValue());
        return toResponse(preference);
    }

    @Transactional
    public UserAnalyticsPreference createDefaultPreference(User user) {
        if (user.getId() != null) {
            return userAnalyticsPreferenceRepository.findByUserId(user.getId())
                    .orElseGet(() -> userAnalyticsPreferenceRepository.save(UserAnalyticsPreference.defaultFor(user, defaultEnabled)));
        }
        return userAnalyticsPreferenceRepository.save(UserAnalyticsPreference.defaultFor(user, defaultEnabled));
    }

    private UserAnalyticsPreference getOrCreatePreference(Long userId) {
        return userAnalyticsPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new GeneralException(USER_NOT_FOUND));
                    return userAnalyticsPreferenceRepository.save(UserAnalyticsPreference.defaultFor(user, defaultEnabled));
                });
    }

    private AnalyticsPreferenceResponseDto toResponse(UserAnalyticsPreference preference) {
        return AnalyticsPreferenceResponseDto.builder()
                .enabled(preference.getEnabled())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
