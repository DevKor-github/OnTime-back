package devkor.ontime_back.repository;

import devkor.ontime_back.entity.AccountDeletionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountDeletionFeedbackRepository extends JpaRepository<AccountDeletionFeedback, UUID> {
}
