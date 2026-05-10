package devkor.ontime_back.global.jwt;

import devkor.ontime_back.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    @DisplayName("공개 HTML 페이지는 액세스 토큰 없이 JWT 필터를 통과한다.")
    @ParameterizedTest
    @ValueSource(strings = {"/account-deletion", "/privacy-policy"})
    void skipsPublicHtmlPages(String path) throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).extractAccessToken(request);
        verify(jwtTokenProvider, never()).extractRefreshToken(request);
    }
}
