package devkor.ontime_back.repository;

import devkor.ontime_back.entity.PreparationTemplate;
import devkor.ontime_back.entity.PreparationTemplateStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreparationTemplateStepRepository extends JpaRepository<PreparationTemplateStep, UUID> {
    @Query("SELECT pts FROM PreparationTemplateStep pts " +
            "WHERE pts.preparationTemplate = :template " +
            "ORDER BY pts.orderIndex ASC, pts.preparationTemplateStepId ASC")
    List<PreparationTemplateStep> findByPreparationTemplateOrdered(@Param("template") PreparationTemplate template);

    @Query("SELECT pts FROM PreparationTemplateStep pts " +
            "JOIN FETCH pts.preparationTemplate pt " +
            "WHERE pts.preparationTemplate IN :templates " +
            "ORDER BY pt.preparationTemplateId ASC, pts.orderIndex ASC, pts.preparationTemplateStepId ASC")
    List<PreparationTemplateStep> findByPreparationTemplatesOrdered(@Param("templates") List<PreparationTemplate> templates);

    void deleteByPreparationTemplate(PreparationTemplate preparationTemplate);

    boolean existsByPreparationTemplateStepIdAndPreparationTemplate(UUID preparationTemplateStepId, PreparationTemplate preparationTemplate);
}
