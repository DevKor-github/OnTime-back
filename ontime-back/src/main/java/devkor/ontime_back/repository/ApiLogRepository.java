package devkor.ontime_back.repository;

import devkor.ontime_back.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
