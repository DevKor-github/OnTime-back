package devkor.ontime_back.repository;

import devkor.ontime_back.entity.UserAnalyticsPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAnalyticsPreferenceRepository extends JpaRepository<UserAnalyticsPreference, Long> {
    Optional<UserAnalyticsPreference> findByUserId(Long userId);
}
