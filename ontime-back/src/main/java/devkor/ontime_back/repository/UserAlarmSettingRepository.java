package devkor.ontime_back.repository;

import devkor.ontime_back.entity.UserAlarmSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAlarmSettingRepository extends JpaRepository<UserAlarmSetting, Long> {
    Optional<UserAlarmSetting> findByUserId(Long userId);
}
