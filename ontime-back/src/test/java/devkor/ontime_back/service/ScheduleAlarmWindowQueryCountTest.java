package devkor.ontime_back.service;

import devkor.ontime_back.SqlStatementCollector;
import devkor.ontime_back.dto.AlarmWindowScheduleDto;
import devkor.ontime_back.entity.DoneStatus;
import devkor.ontime_back.entity.Place;
import devkor.ontime_back.entity.PreparationMode;
import devkor.ontime_back.entity.PreparationUser;
import devkor.ontime_back.entity.Schedule;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.PlaceRepository;
import devkor.ontime_back.repository.PreparationUserRepository;
import devkor.ontime_back.repository.ScheduleRepository;
import devkor.ontime_back.repository.UserAlarmSettingRepository;
import devkor.ontime_back.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=devkor.ontime_back.SqlStatementCollector"
})
@Transactional
class ScheduleAlarmWindowQueryCountTest {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private PreparationUserRepository preparationUserRepository;

    @Autowired
    private UserAlarmSettingRepository userAlarmSettingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void tearDown() {
        userAlarmSettingRepository.deleteAllInBatch();
        preparationUserRepository.deleteAllInBatch();
        scheduleRepository.deleteAllInBatch();
        placeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("알람 윈도우 DEFAULT 준비과정 조회는 스케줄 수에 비례해 SQL이 증가하지 않는다")
    void alarmWindowDefaultPreparationQueryCountDoesNotScaleWithScheduleCount() {
        int scheduleCount = 25;
        User user = createUser();
        Place place = placeRepository.save(new Place(UUID.randomUUID(), "연구실"));
        createDefaultPreparations(user);
        createDefaultModeSchedules(user, place, scheduleCount);

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = statistics();
        statistics.clear();
        SqlStatementCollector.clear();

        List<AlarmWindowScheduleDto> result = scheduleService.getAlarmWindowSchedules(
                user.getId(),
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 15, 0, 0)
        );

        long statementCount = statistics.getPrepareStatementCount();
        System.out.printf("alarm-window DEFAULT schedules=%d preparedStatements=%d%n", scheduleCount, statementCount);
        SqlStatementCollector.statements().stream()
                .collect(Collectors.groupingBy(sql -> sql, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> System.out.printf("sqlCount=%d sql=%s%n", entry.getValue(), entry.getKey()));

        assertThat(result).hasSize(scheduleCount);
        assertThat(result)
                .allSatisfy(schedule -> assertThat(schedule.getPreparations()).hasSize(3));
        assertThat(statementCount)
                .as("1 schedule query + 1 alarm setting query + 1 user spare-time query + 1 default preparation query")
                .isLessThanOrEqualTo(4L);
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .email("alarm-window@example.com")
                .password("password")
                .name("alarm-user")
                .spareTime(10)
                .punctualityScore(-1f)
                .scheduleCountAfterReset(0)
                .latenessCountAfterReset(0)
                .build());
    }

    private void createDefaultPreparations(User user) {
        PreparationUser third = preparationUserRepository.save(PreparationUser.builder()
                .preparationUserId(UUID.randomUUID())
                .user(user)
                .preparationName("가방 챙기기")
                .preparationTime(5)
                .orderIndex(2)
                .build());
        PreparationUser second = preparationUserRepository.save(PreparationUser.builder()
                .preparationUserId(UUID.randomUUID())
                .user(user)
                .preparationName("옷 입기")
                .preparationTime(10)
                .orderIndex(1)
                .nextPreparation(third)
                .build());
        preparationUserRepository.save(PreparationUser.builder()
                .preparationUserId(UUID.randomUUID())
                .user(user)
                .preparationName("세수하기")
                .preparationTime(10)
                .orderIndex(0)
                .nextPreparation(second)
                .build());
    }

    private void createDefaultModeSchedules(User user, Place place, int scheduleCount) {
        LocalDateTime baseTime = LocalDateTime.of(2026, 7, 1, 9, 0);
        for (int i = 0; i < scheduleCount; i++) {
            scheduleRepository.save(Schedule.builder()
                    .scheduleId(UUID.randomUUID())
                    .user(user)
                    .place(place)
                    .scheduleName("회의 " + i)
                    .moveTime(20)
                    .scheduleTime(baseTime.plusHours(i))
                    .isChange(false)
                    .preparationMode(PreparationMode.DEFAULT)
                    .scheduleSpareTime(null)
                    .latenessTime(-1)
                    .doneStatus(DoneStatus.NOT_ENDED)
                    .build());
        }
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
