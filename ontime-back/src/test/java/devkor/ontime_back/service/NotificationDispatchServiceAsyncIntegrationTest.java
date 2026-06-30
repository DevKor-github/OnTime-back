package devkor.ontime_back.service;

import devkor.ontime_back.config.SchedulerConfig;
import devkor.ontime_back.entity.NotificationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.NotificationScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(NotificationDispatchServiceAsyncIntegrationTest.Config.class)
class NotificationDispatchServiceAsyncIntegrationTest {
    private static final long BLOCKING_DELIVERY_MS = 300;
    private static final long MAX_DISPATCH_RETURN_MS = 100;

    @Autowired
    private NotificationDispatchService notificationDispatchService;

    @Autowired
    private NotificationScheduleRepository notificationScheduleRepository;

    @Autowired
    private NotificationDeliveryService notificationDeliveryService;

    @BeforeEach
    void setUp() {
        reset(notificationScheduleRepository, notificationDeliveryService);
    }

    @Test
    void dispatchReminderReturnsQuicklyWhileDeliveryRunsOnNotificationExecutorInTransaction() throws Exception {
        NotificationSchedule notification = notificationSchedule();
        CountDownLatch deliveryStarted = new CountDownLatch(1);
        CountDownLatch deliveryFinished = new CountDownLatch(1);
        AtomicReference<String> deliveryThreadName = new AtomicReference<>();
        AtomicBoolean transactionActive = new AtomicBoolean(false);

        when(notificationScheduleRepository.findByIdWithScheduleAndUser(10L))
                .thenReturn(Optional.of(notification));
        doAnswer(invocation -> {
            deliveryThreadName.set(Thread.currentThread().getName());
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            deliveryStarted.countDown();
            try {
                Thread.sleep(BLOCKING_DELIVERY_MS);
            } finally {
                deliveryFinished.countDown();
            }
            return null;
        }).when(notificationDeliveryService).sendReminder(notification, "message");

        long startedAt = System.nanoTime();

        notificationDispatchService.dispatchReminder(10L, "message");

        long dispatchReturnMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertThat(dispatchReturnMs).isLessThan(MAX_DISPATCH_RETURN_MS);
        assertThat(deliveryStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(deliveryFinished.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(deliveryThreadName.get()).startsWith("notification-");
        assertThat(transactionActive.get()).isTrue();
    }

    private NotificationSchedule notificationSchedule() {
        return NotificationSchedule.builder()
                .notificationTime(LocalDateTime.now())
                .isSent(false)
                .schedule(Schedule.builder()
                        .scheduleId(UUID.randomUUID())
                        .scheduleName("Morning meeting")
                        .user(User.builder()
                                .id(1L)
                                .name("User")
                                .firebaseToken("firebase-token")
                                .build())
                        .build())
                .build();
    }

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    @Import({SchedulerConfig.class, NotificationDispatchService.class})
    static class Config {
        @Bean
        NotificationScheduleRepository notificationScheduleRepository() {
            return mock(NotificationScheduleRepository.class);
        }

        @Bean
        NotificationDeliveryService notificationDeliveryService() {
            return mock(NotificationDeliveryService.class);
        }

        @Bean
        TestTransactionManager transactionManager() {
            return new TestTransactionManager();
        }
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
