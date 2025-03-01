package devkor.ontime_back.repository;

import devkor.ontime_back.entity.PreparationSchedule;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreparationScheduleRepository extends JpaRepository<PreparationSchedule, UUID> {

    @Query("SELECT ps FROM PreparationSchedule ps " +
            "LEFT JOIN FETCH ps.nextPreparation " +
            "WHERE ps.schedule = :schedule")
    List<PreparationSchedule> findByScheduleWithNextPreparation(@Param("schedule") Schedule schedule);

    void deleteBySchedule(Schedule schedule);
}
