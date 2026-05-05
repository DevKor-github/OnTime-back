package devkor.ontime_back.repository;

import devkor.ontime_back.entity.UserAlarmStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAlarmStatusRepository extends JpaRepository<UserAlarmStatus, Long> {
    Optional<UserAlarmStatus> findByUserDeviceUserDeviceId(Long userDeviceId);
}
