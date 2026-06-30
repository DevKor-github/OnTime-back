package devkor.ontime_back.global.jwt;

import devkor.ontime_back.entity.Role;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.service.AuthTokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class JwtAuthenticationQueryCountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void protectedPunctualityScoreRequestStaysWithinThePostFixSqlBudget() throws Exception {
        User user = userRepository.saveAndFlush(user());
        AuthTokenService.AuthTokens tokens = authTokenService.issueLoginTokens(user, new MockHttpServletResponse());
        userRepository.saveAndFlush(user);
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/users/me/punctuality-score")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
    }

    private User user() {
        return User.builder()
                .email("jwt-query-count@example.com")
                .password("password")
                .name("jwt-query-count-user")
                .punctualityScore(100F)
                .role(Role.USER)
                .build();
    }
}
