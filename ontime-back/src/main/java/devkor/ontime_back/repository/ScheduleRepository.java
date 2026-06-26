package devkor.ontime_back.repository;


import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    @Query("SELECT s FROM Schedule s JOIN FETCH s.place WHERE s.scheduleId = :scheduleId")
    Optional<Schedule> findByIdWithPlace(@Param("scheduleId")UUID scheduleId);

    // getScheduleWithAuthorization에서 Lazy Loading으로 인해 추가 SELECT 발생 가능 -> JOIN FETCH를 사용하여 Schedule과 User를 한 번의 쿼리로 조회
    @Query("SELECT s FROM Schedule s JOIN FETCH s.user WHERE s.scheduleId = :scheduleId")
    Optional<Schedule> findByIdWithUser(@Param("scheduleId") UUID scheduleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s JOIN FETCH s.user LEFT JOIN FETCH s.place WHERE s.scheduleId = :scheduleId")
    Optional<Schedule> findByIdWithUserAndPlaceForUpdate(@Param("scheduleId") UUID scheduleId);

    // deleteById()는 내부적으로 findById()를 실행하여 엔티티를 로드 후 삭제 -> JPQL DELETE를 사용해 한 번의 DELETE 쿼리만 실행
    @Modifying
    @Query("DELETE FROM Schedule s WHERE s.scheduleId = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.place WHERE s.user.id = :userId")
    List<Schedule> findAllByUserIdWithPlace(Long userId);
    @Query("SELECT s FROM Schedule s JOIN FETCH s.place WHERE s.user.id = :userId AND s.scheduleTime < :endDate")
    List<Schedule> findAllByUserIdAndScheduleTimeBefore(@Param("userId") Long userId, @Param("endDate") LocalDateTime endDate);
    @Query("SELECT s FROM Schedule s JOIN FETCH s.place WHERE s.user.id = :userId AND s.scheduleTime > :startDate")
    List<Schedule> findAllByUserIdAndScheduleTimeAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    @Query("SELECT s FROM Schedule s JOIN FETCH s.place WHERE s.user.id = :userId AND s.scheduleTime BETWEEN :startDate AND :endDate")
    List<Schedule> findAllByUserIdAndScheduleTimeBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 특정 시간 범위 내에 시작되는 약속 조회
    @Query("SELECT s FROM Schedule s WHERE s.scheduleTime BETWEEN :start AND :end")
    List<Schedule> findSchedulesBetween(LocalDateTime start, LocalDateTime end);


    // 지각 히스토리 조회(페치조인을 했었는데 본 메서드를 사용하는 서비스메서드에서 user를 참조하지 않아서 필요없음)
    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId AND s.latenessTime > 0")
    List<Schedule> findLatenessHistoryByUserId(@Param("userId") Long userId);

    // 약속 히스토리 조회
    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId")
    List<Schedule> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Schedule s " +
            "JOIN FETCH s.user " +
            "LEFT JOIN FETCH s.place " +
            "WHERE s.user.id = :userId " +
            "AND s.doneStatus = :doneStatus " +
            "AND s.scheduleTime >= :startDate " +
            "AND s.scheduleTime < :endDate " +
            "ORDER BY s.scheduleTime ASC, s.scheduleId ASC")
    List<Schedule> findAlarmWindowSchedules(@Param("userId") Long userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("doneStatus") DoneStatus doneStatus);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.user " +
            "WHERE s.startedAt IS NOT NULL " +
            "AND NOT EXISTS (SELECT ps FROM PreparationSchedule ps WHERE ps.schedule = s)")
    List<Schedule> findStartedSchedulesWithoutPreparationSnapshot();

    @Query("SELECT s FROM Schedule s " +
            "JOIN FETCH s.user " +
            "LEFT JOIN FETCH s.place " +
            "LEFT JOIN FETCH s.preparationTemplate " +
            "WHERE s.preparationMode = devkor.ontime_back.entity.PreparationMode.TEMPLATE " +
            "AND s.preparationTemplate.preparationTemplateId = :templateId " +
            "AND s.doneStatus = devkor.ontime_back.entity.DoneStatus.NOT_ENDED " +
            "AND s.startedAt IS NULL")
    List<Schedule> findNotStartedTemplateModeSchedules(@Param("templateId") UUID templateId);

    @Query("SELECT s FROM Schedule s " +
            "JOIN FETCH s.user " +
            "LEFT JOIN FETCH s.place " +
            "WHERE s.user.id = :userId " +
            "AND s.preparationMode = devkor.ontime_back.entity.PreparationMode.DEFAULT " +
            "AND s.doneStatus = devkor.ontime_back.entity.DoneStatus.NOT_ENDED " +
            "AND s.startedAt IS NULL")
    List<Schedule> findNotStartedDefaultModeSchedules(@Param("userId") Long userId);

}
