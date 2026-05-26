package devkor.ontime_back.service;

import devkor.ontime_back.dto.AnalyticsPreferenceResponseDto;
import devkor.ontime_back.dto.AnalyticsPreferenceUpdateDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.UserAnalyticsPreference;
import devkor.ontime_back.repository.UserAnalyticsPreferenceRepository;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsPreferenceServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAnalyticsPreferenceRepository userAnalyticsPreferenceRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .build();
    }

    @Test
    @DisplayName("설정 행이 없으면 현재 배포 기본값 false로 생성해 반환한다")
    void getAnalyticsPreferenceCreatesDefaultDisabledPreference() {
        AnalyticsPreferenceService service = serviceWithDefault(false);
        when(userAnalyticsPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userAnalyticsPreferenceRepository.save(any(UserAnalyticsPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalyticsPreferenceResponseDto response = service.getAnalyticsPreference(USER_ID);

        assertThat(response.getEnabled()).isFalse();
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(userAnalyticsPreferenceRepository).save(any(UserAnalyticsPreference.class));
    }

    @Test
    @DisplayName("사용자가 업데이트하면 값을 저장하고 userOverridden과 updatedAt을 갱신한다")
    void updateAnalyticsPreferenceMarksUserOverride() {
        AnalyticsPreferenceService service = serviceWithDefault(false);
        Instant previousUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        UserAnalyticsPreference preference = preference(false, previousUpdatedAt, false);
        AnalyticsPreferenceUpdateDto requestDto = new AnalyticsPreferenceUpdateDto();
        ReflectionTestUtils.setField(requestDto, "enabled", true);

        when(userAnalyticsPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(preference));

        AnalyticsPreferenceResponseDto response = service.updateAnalyticsPreference(USER_ID, requestDto);

        assertThat(response.getEnabled()).isTrue();
        assertThat(response.getUpdatedAt()).isAfter(previousUpdatedAt);
        assertThat(preference.getUserOverridden()).isTrue();
    }

    @Test
    @DisplayName("사용자가 건드리지 않은 행은 배포 기본값이 true로 바뀌면 읽을 때 정렬된다")
    void getAnalyticsPreferenceAlignsNonOverriddenRowsToCurrentDefault() {
        AnalyticsPreferenceService service = serviceWithDefault(true);
        Instant previousUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        UserAnalyticsPreference preference = preference(false, previousUpdatedAt, false);

        when(userAnalyticsPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(preference));

        AnalyticsPreferenceResponseDto response = service.getAnalyticsPreference(USER_ID);

        assertThat(response.getEnabled()).isTrue();
        assertThat(response.getUpdatedAt()).isAfter(previousUpdatedAt);
        assertThat(preference.getUserOverridden()).isFalse();
    }

    @Test
    @DisplayName("명시적으로 끈 사용자의 선택은 배포 기본값 true에도 보존된다")
    void getAnalyticsPreferencePreservesExplicitOptOut() {
        AnalyticsPreferenceService service = serviceWithDefault(true);
        Instant previousUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        UserAnalyticsPreference preference = preference(false, previousUpdatedAt, true);

        when(userAnalyticsPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.of(preference));

        AnalyticsPreferenceResponseDto response = service.getAnalyticsPreference(USER_ID);

        assertThat(response.getEnabled()).isFalse();
        assertThat(response.getUpdatedAt()).isEqualTo(previousUpdatedAt);
        assertThat(preference.getUserOverridden()).isTrue();
    }

    @Test
    @DisplayName("설정 행도 사용자도 없으면 USER_NOT_FOUND를 반환한다")
    void getAnalyticsPreferenceRejectsMissingUser() {
        AnalyticsPreferenceService service = serviceWithDefault(false);
        when(userAnalyticsPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAnalyticsPreference(USER_ID))
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private AnalyticsPreferenceService serviceWithDefault(boolean defaultEnabled) {
        return new AnalyticsPreferenceService(userRepository, userAnalyticsPreferenceRepository, defaultEnabled);
    }

    private UserAnalyticsPreference preference(boolean enabled, Instant updatedAt, boolean userOverridden) {
        return UserAnalyticsPreference.builder()
                .user(user)
                .enabled(enabled)
                .updatedAt(updatedAt)
                .userOverridden(userOverridden)
                .build();
    }
}
