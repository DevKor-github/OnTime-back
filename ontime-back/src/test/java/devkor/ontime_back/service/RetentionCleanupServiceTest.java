package devkor.ontime_back.service;

import devkor.ontime_back.entity.AccountDeletionFeedback;
import devkor.ontime_back.entity.ApiLog;
import devkor.ontime_back.entity.SocialType;
import devkor.ontime_back.repository.AccountDeletionFeedbackRepository;
import devkor.ontime_back.repository.ApiLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetentionCleanupServiceTest {

    @Autowired
    private RetentionCleanupService retentionCleanupService;

    @Autowired
    private AccountDeletionFeedbackRepository accountDeletionFeedbackRepository;

    @Autowired
    private ApiLogRepository apiLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        accountDeletionFeedbackRepository.deleteAllInBatch();
        apiLogRepository.deleteAllInBatch();
    }

    @DisplayName("탈퇴 피드백은 1년보다 오래된 행만 삭제한다")
    @Test
    void cleanupDeletesOnlyAccountDeletionFeedbackOlderThanOneYear() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0);
        UUID expiredFeedbackId = UUID.randomUUID();
        UUID cutoffFeedbackId = UUID.randomUUID();
        UUID retainedFeedbackId = UUID.randomUUID();

        accountDeletionFeedbackRepository.save(AccountDeletionFeedback.builder()
                .feedbackId(expiredFeedbackId)
                .deletedUserId(1L)
                .socialType(SocialType.GOOGLE)
                .emailHash("a".repeat(64))
                .message("expired feedback")
                .createdAt(now.minusYears(1).minusSeconds(1))
                .build());
        accountDeletionFeedbackRepository.save(AccountDeletionFeedback.builder()
                .feedbackId(cutoffFeedbackId)
                .deletedUserId(2L)
                .socialType(SocialType.APPLE)
                .emailHash("b".repeat(64))
                .message("cutoff feedback")
                .createdAt(now.minusYears(1))
                .build());
        accountDeletionFeedbackRepository.save(AccountDeletionFeedback.builder()
                .feedbackId(retainedFeedbackId)
                .deletedUserId(3L)
                .socialType(SocialType.KAKAO)
                .emailHash("c".repeat(64))
                .message("retained feedback")
                .createdAt(now.minusYears(1).plusSeconds(1))
                .build());

        // when
        RetentionCleanupService.RetentionCleanupResult result = retentionCleanupService.cleanupExpiredRetentionData(now);

        // then
        assertThat(result.deletedAccountDeletionFeedback()).isEqualTo(1);
        assertThat(accountDeletionFeedbackRepository.findById(expiredFeedbackId)).isEmpty();
        assertThat(accountDeletionFeedbackRepository.findById(cutoffFeedbackId)).isPresent();
        assertThat(accountDeletionFeedbackRepository.findById(retainedFeedbackId)).isPresent();
    }

    @DisplayName("API 로그는 90일보다 오래된 행만 삭제한다")
    @Test
    void cleanupDeletesOnlyApiLogsOlderThanNinetyDays() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0);
        ApiLog expiredLog = apiLogRepository.save(ApiLog.builder()
                .requestUrl("/expired")
                .requestMethod("GET")
                .userId("1")
                .clientIp("127.0.0.1")
                .responseStatus(200)
                .takenTime(10L)
                .build());
        ApiLog cutoffLog = apiLogRepository.save(ApiLog.builder()
                .requestUrl("/cutoff")
                .requestMethod("GET")
                .userId("2")
                .clientIp("127.0.0.1")
                .responseStatus(200)
                .takenTime(20L)
                .build());
        ApiLog retainedLog = apiLogRepository.save(ApiLog.builder()
                .requestUrl("/retained")
                .requestMethod("GET")
                .userId("3")
                .clientIp("127.0.0.1")
                .responseStatus(200)
                .takenTime(30L)
                .build());
        setApiLogCreatedAt(expiredLog.getApiLogId(), now.minusDays(90).minusSeconds(1));
        setApiLogCreatedAt(cutoffLog.getApiLogId(), now.minusDays(90));
        setApiLogCreatedAt(retainedLog.getApiLogId(), now.minusDays(90).plusSeconds(1));

        // when
        RetentionCleanupService.RetentionCleanupResult result = retentionCleanupService.cleanupExpiredRetentionData(now);

        // then
        assertThat(result.deletedApiLogs()).isEqualTo(1);
        assertThat(apiLogRepository.findById(expiredLog.getApiLogId())).isEmpty();
        assertThat(apiLogRepository.findById(cutoffLog.getApiLogId())).isPresent();
        assertThat(apiLogRepository.findById(retainedLog.getApiLogId())).isPresent();
    }

    private void setApiLogCreatedAt(Long apiLogId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE api_log SET created_at = ? WHERE api_log_id = ?",
                createdAt,
                apiLogId
        );
    }
}
