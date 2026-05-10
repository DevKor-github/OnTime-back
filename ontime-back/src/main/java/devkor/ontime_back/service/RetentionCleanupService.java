package devkor.ontime_back.service;

import devkor.ontime_back.repository.AccountDeletionFeedbackRepository;
import devkor.ontime_back.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionCleanupService {

    private static final int ACCOUNT_DELETION_FEEDBACK_RETENTION_YEARS = 1;
    private static final int API_LOG_RETENTION_DAYS = 90;

    private final AccountDeletionFeedbackRepository accountDeletionFeedbackRepository;
    private final ApiLogRepository apiLogRepository;

    @Scheduled(cron = "0 30 3 * * *")
    public void cleanupExpiredRetentionData() {
        cleanupExpiredRetentionData(LocalDateTime.now());
    }

    @Transactional
    public RetentionCleanupResult cleanupExpiredRetentionData(LocalDateTime now) {
        LocalDateTime accountDeletionFeedbackCutoff = now.minusYears(ACCOUNT_DELETION_FEEDBACK_RETENTION_YEARS);
        LocalDateTime apiLogCutoff = now.minusDays(API_LOG_RETENTION_DAYS);

        long deletedAccountDeletionFeedback = accountDeletionFeedbackRepository.deleteByCreatedAtBefore(accountDeletionFeedbackCutoff);
        long deletedApiLogs = apiLogRepository.deleteByCreatedAtBefore(apiLogCutoff);

        log.info("Retention cleanup completed. deletedAccountDeletionFeedback: {}, deletedApiLogs: {}",
                deletedAccountDeletionFeedback, deletedApiLogs);

        return new RetentionCleanupResult(deletedAccountDeletionFeedback, deletedApiLogs);
    }

    public record RetentionCleanupResult(long deletedAccountDeletionFeedback, long deletedApiLogs) {
    }
}
