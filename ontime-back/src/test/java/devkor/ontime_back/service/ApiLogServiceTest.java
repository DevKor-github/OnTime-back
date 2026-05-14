package devkor.ontime_back.service;

import devkor.ontime_back.entity.ApiLog;
import devkor.ontime_back.repository.ApiLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiLogServiceTest {

    @Mock
    private ApiLogRepository apiLogRepository;

    private ApiLogService apiLogService;

    @BeforeEach
    void setUp() {
        apiLogService = new ApiLogService(apiLogRepository);
    }

    @Test
    void saveLogPersistsStructuredRequestAuditRecord() {
        ApiLog apiLog = apiLog();

        apiLogService.saveLog(apiLog);

        verify(apiLogRepository).save(apiLog);
    }

    @Test
    void saveLogDoesNotBreakRequestFlowWhenAuditPersistenceFails() {
        ApiLog apiLog = apiLog();
        doThrow(new RuntimeException("database unavailable")).when(apiLogRepository).save(apiLog);

        apiLogService.saveLog(apiLog);

        verify(apiLogRepository).save(apiLog);
    }

    private ApiLog apiLog() {
        return ApiLog.builder()
                .requestUrl("/schedules")
                .requestMethod("GET")
                .userId("1")
                .clientIp("127.0.0.1")
                .responseStatus(200)
                .takenTime(25L)
                .build();
    }
}
