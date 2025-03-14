package devkor.ontime_back.repository;

import devkor.ontime_back.entity.User;
import devkor.ontime_back.entity.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByName(String name);

    Optional<User> findByRefreshToken(String refreshToken);

    // socialType과 socialId으로 user 찾는 메소드
    // 추가정보 입력받을때 사용
    Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

    @Query("SELECT u.spareTime FROM User u WHERE u.id = :id")
    Integer findSpareTimeById(Long id);
}