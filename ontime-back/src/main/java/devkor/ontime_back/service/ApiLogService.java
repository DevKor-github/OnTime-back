package devkor.ontime_back.service;

import devkor.ontime_back.entity.ApiLog;
import devkor.ontime_back.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiLogService {
    private final ApiLogRepository apiLogRepository;

    @Async
    public void saveLog(ApiLog apiLog) {
        try {
            log.info("[Async] saveLog started on thread: {}", Thread.currentThread().getName());
            apiLogRepository.save(apiLog);
            log.info("[Async] saveLog finished on thread: {}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("API 로그 저장 실패", e);
        }
    }
}
