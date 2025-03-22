package devkor.ontime_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.dto.PreparationDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
public class PreparationScheduleControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("스케줄별 준비과정 생성에 성공한다.")
    void createPreparationSchedule_success() throws Exception {
        // given
        UUID scheduleId = UUID.randomUUID();
        Long userId = 1L;

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(UUID.randomUUID(), "세면", 10, UUID.randomUUID()),
                new PreparationDto(UUID.randomUUID(), "옷입기", 15, null)
        );

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        doNothing().when(preparationScheduleService).makePreparationSchedules(eq(userId), eq(scheduleId), any());

        // when & then
        mockMvc.perform(post("/preparation-schedules/{scheduleId}", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(preparationDtoList))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(preparationScheduleService, times(1))
                .makePreparationSchedules(eq(userId), eq(scheduleId), argThat(list -> list.size() == preparationDtoList.size()));
    }

    @Test
    @DisplayName("스케줄별 준비과정 수정에 성공한다.")
    void modifyPreparationSchedule_success() throws Exception {
        // given
        UUID scheduleId = UUID.randomUUID();
        Long userId = 1L;

        List<PreparationDto> preparationDtoList = List.of(
                new PreparationDto(UUID.randomUUID(), "세면", 10, UUID.randomUUID()),
                new PreparationDto(UUID.randomUUID(), "옷입기", 15, null)
        );

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        doNothing().when(preparationScheduleService).updatePreparationSchedules(eq(userId), eq(scheduleId), any());

        // when & then
        mockMvc.perform(put("/preparation-schedules/{scheduleId}", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(preparationDtoList))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(preparationScheduleService, times(1))
                .updatePreparationSchedules(eq(userId), eq(scheduleId), argThat(list -> list.size() == preparationDtoList.size()));
    }

}
