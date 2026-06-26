package devkor.ontime_back.controller;

import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.dto.AnalyticsPreferenceResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class AnalyticsPreferenceControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("분석 설정 조회는 로그인한 계정의 enabled와 updatedAt을 반환한다")
    void getAnalyticsPreference() throws Exception {
        Instant updatedAt = Instant.parse("2026-05-26T12:00:00Z");
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(analyticsPreferenceService.getAnalyticsPreference(1L)).thenReturn(
                AnalyticsPreferenceResponseDto.builder()
                        .enabled(false)
                        .updatedAt(updatedAt)
                        .build()
        );

        mockMvc.perform(get("/users/me/analytics-preference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-05-26T12:00:00Z"));

        verify(analyticsPreferenceService).getAnalyticsPreference(1L);
    }

    @Test
    @DisplayName("분석 설정 업데이트는 boolean enabled만 받아 계정 설정을 변경한다")
    void updateAnalyticsPreference() throws Exception {
        Instant updatedAt = Instant.parse("2026-05-26T12:00:05Z");
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(analyticsPreferenceService.updateAnalyticsPreference(eq(1L), any())).thenReturn(
                AnalyticsPreferenceResponseDto.builder()
                        .enabled(true)
                        .updatedAt(updatedAt)
                        .build()
        );

        mockMvc.perform(put("/users/me/analytics-preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-05-26T12:00:05Z"));

        verify(analyticsPreferenceService).updateAnalyticsPreference(eq(1L), any());
    }

    @Test
    @DisplayName("분석 설정 업데이트는 누락, null, 문자열, 알 수 없는 필드를 거절한다")
    void updateAnalyticsPreferenceValidationFailure() throws Exception {
        assertInvalidUpdate("{}");
        assertInvalidUpdate("{\"enabled\":null}");
        assertInvalidUpdate("{\"enabled\":\"false\"}");
        assertInvalidUpdate("{\"enabled\":false,\"unknown\":1}");

        verify(analyticsPreferenceService, never()).updateAnalyticsPreference(any(), any());
    }

    private void assertInvalidUpdate(String body) throws Exception {
        mockMvc.perform(put("/users/me/analytics-preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.data.errors").isArray());
    }
}
