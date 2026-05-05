package devkor.ontime_back.repository;

import devkor.ontime_back.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserDevice> findByUserIdAndDeviceIdAndActiveTrue(Long userId, String deviceId);

    Optional<UserDevice> findFirstByUserIdAndActiveTrueOrderByLastSeenAtDesc(Long userId);

    List<UserDevice> findAllByUserIdAndActiveTrue(Long userId);
}
