package devkor.ontime_back.repository;

import devkor.ontime_back.entity.PreparationUser;
import devkor.ontime_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreparationUserRepository extends JpaRepository<PreparationUser, UUID> {

    // user 객체를 생성하는 대신, userId로 preparationUser 가져오는 메서드 생성
    @Query("SELECT pu FROM PreparationUser pu " +
            "LEFT JOIN FETCH pu.nextPreparation " +
            "WHERE pu.user.id = :userId")
    List<PreparationUser> findByUserIdWithNextPreparation(@Param("userId") Long userId);

    @Query("SELECT pu FROM PreparationUser pu " +
            "LEFT JOIN FETCH pu.nextPreparation " +
            "WHERE pu.user.id = :userId " +
            "AND pu NOT IN (SELECT p.nextPreparation FROM PreparationUser p WHERE p.nextPreparation IS NOT NULL)")
    Optional<PreparationUser> findFirstPreparationUserByUserIdWithNextPreparation(@Param("userId") Long userId);

    void deleteByUser(User user);

    @Modifying
    @Query("UPDATE PreparationUser p SET p.nextPreparation = NULL WHERE p.user.id = :userId")
    void clearNextPreparationByUserId(@Param("userId") Long userId);

    boolean existsByUser(User user);

    List<PreparationUser> findByUser(User user);
}
