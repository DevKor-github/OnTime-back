package devkor.ontime_back.repository;

import devkor.ontime_back.entity.PreparationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreparationTemplateRepository extends JpaRepository<PreparationTemplate, UUID> {
    @Query("SELECT pt FROM PreparationTemplate pt " +
            "WHERE pt.user.id = :userId AND pt.deletedAt IS NULL " +
            "ORDER BY pt.createdAt ASC, pt.preparationTemplateId ASC")
    List<PreparationTemplate> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT pt FROM PreparationTemplate pt WHERE pt.preparationTemplateId = :templateId AND pt.user.id = :userId")
    Optional<PreparationTemplate> findByIdAndUserId(@Param("templateId") UUID templateId, @Param("userId") Long userId);

    @Query("SELECT pt FROM PreparationTemplate pt " +
            "WHERE pt.preparationTemplateId = :templateId AND pt.user.id = :userId AND pt.deletedAt IS NULL")
    Optional<PreparationTemplate> findActiveByIdAndUserId(@Param("templateId") UUID templateId, @Param("userId") Long userId);

    boolean existsByUser_IdAndNormalizedTemplateNameAndDeletedAtIsNull(Long userId, String normalizedTemplateName);

    boolean existsByUser_IdAndNormalizedTemplateNameAndDeletedAtIsNullAndPreparationTemplateIdNot(Long userId, String normalizedTemplateName, UUID preparationTemplateId);

    long countByUser_IdAndDeletedAtIsNull(Long userId);
}
