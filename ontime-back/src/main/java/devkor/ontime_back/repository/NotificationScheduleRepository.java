package devkor.ontime_back.repository;

import devkor.ontime_back.entity.NotificationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {
    @Query("SELECT n FROM NotificationSchedule n " +
            "JOIN FETCH n.schedule s " +
            "JOIN FETCH s.user " +
            "WHERE n.notificationTime > :now AND n.isSent = false")
    List<NotificationSchedule> findAllWithScheduleAndUser(LocalDateTime now);

    Optional<NotificationSchedule> findByScheduleId(UUID scheduleId);
}
