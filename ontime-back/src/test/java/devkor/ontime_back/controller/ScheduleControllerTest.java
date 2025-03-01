package devkor.ontime_back.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class ScheduleControllerTest extends ControllerTestSupport {

    private String json(Object object) throws JsonProcessingException {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(object);
    }

    @DisplayName("사용자의 특정 기간 일정 조회에 성공한다.")
    @Test
    void getPeriodSchedule_success() throws Exception {
        // given
        Long userId = 1L;
        LocalDateTime startDate = LocalDateTime.of(2024, 11, 15, 18, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 11, 18, 20, 0);

        List<ScheduleDto> mockSchedules = List.of(
                new ScheduleDto(UUID.randomUUID(), new PlaceDto(UUID.randomUUID(), "과학도서관"), "공부하기", 10, startDate, 5, "늦으면 안됨", 2),
                new ScheduleDto(UUID.randomUUID(), new PlaceDto(UUID.randomUUID(), "중식당"), "가족행사", 15, startDate.plusHours(2), 10, "생신", 0)
        );
        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        when(scheduleService.showSchedulesByPeriod(userId, startDate, endDate)).thenReturn(mockSchedules);

        // when & then
        mockMvc.perform(get("/schedule/show")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].scheduleName").value("공부하기"))
                .andExpect(jsonPath("$.data[1].scheduleName").value("가족행사"))
                .andDo(print());

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(scheduleService, times(1)).showSchedulesByPeriod(userId, startDate, endDate);

    }

    @DisplayName("일정 id로 일정 조회에 성공한다.")
    @Test
    void getScheduleById_success() throws Exception {
        // given
        Long userId = 1L;
        UUID scheduleId = UUID.randomUUID();

        ScheduleDto mockSchedule = new ScheduleDto(scheduleId, new PlaceDto(UUID.randomUUID(), "과학도서관"), "공부하기", 10, LocalDateTime.of(2024, 11, 15, 18, 0), 5, "늦으면 안됨", 2);

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        when(scheduleService.showScheduleByScheduleId(userId, scheduleId)).thenReturn(mockSchedule);

        // when & then
        mockMvc.perform(get("/schedule/show/id")
                        .param("scheduleId", scheduleId.toString())
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.scheduleId").value(scheduleId.toString()))
                .andExpect(jsonPath("$.data.scheduleName").value("공부하기"))
                .andDo(print());

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(scheduleService, times(1)).showScheduleByScheduleId(userId, scheduleId);
    }

    @DisplayName("약속 삭제를 성공한다.")
    @Test
    void deleteSchedule_success() throws Exception {
        // given
        UUID scheduleId = UUID.randomUUID();
        Long userId = 1L;

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        doNothing().when(scheduleService).deleteSchedule(scheduleId, userId);

        // when & then
        mockMvc.perform(delete("/schedule/delete/{scheduleId}", scheduleId)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andDo(print());

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(scheduleService, times(1)).deleteSchedule(scheduleId, userId);
    }

    @DisplayName("약속 수정을 성공한다.")
    @Test
    void modifySchedule_success() throws Exception {
        // given
        Long userId = 1L;

        ScheduleModDto scheduleModDto = new ScheduleModDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "애기능생활관",
                "학식먹기",
                10,
                LocalDateTime.of(2024, 11, 20, 10, 0),
                20,
                5,
                "점심 식단 확인하자."
        );

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        doNothing().when(scheduleService).modifySchedule(userId, scheduleModDto);

        // when & then
        mockMvc.perform(put("/schedule/modify")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(scheduleModDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andDo(print());

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(scheduleService, times(1)).modifySchedule(eq(userId), any(ScheduleModDto.class));
    }

    @DisplayName("약속 추가를 성공한다.")
    @Test
    void addSchedule_success() throws Exception {
        // given
        Long userId = 1L;
        LocalDateTime localDateTime = LocalDateTime.of(2024, 11, 25, 14, 0);

        ScheduleAddDto scheduleAddDto = new ScheduleAddDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "과학도서관",
                "공부하기",
                10,
                localDateTime,
                false,
                false,
                5,
                "늦으면 안됨"
        );

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        doNothing().when(scheduleService).addSchedule(scheduleAddDto, userId);

        // when & then
        mockMvc.perform(post("/schedule/add")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(scheduleAddDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andDo(print());

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(scheduleService, times(1)).addSchedule(any(ScheduleAddDto.class), eq(userId));
    }

    @DisplayName("약속 시작을 성공한다.")
    @Test
    void isStartedSchedule_success() throws Exception {
        // given
        UUID scheduleId = UUID.randomUUID();
        Long userId = 1L;

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        doNothing().when(scheduleService).checkIsStarted(scheduleId, userId);

        // when & then
        mockMvc.perform(patch("/schedule/start/{scheduleId}", scheduleId)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andDo(print());

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(scheduleService, times(1)).checkIsStarted(scheduleId, userId);
    }

    @DisplayName("준비과정 조회를 성공한다.")
    @Test
    void getPreparation_success() throws Exception {
        // given
        UUID scheduleId = UUID.randomUUID();
        UUID preparationId1 = UUID.randomUUID();
        UUID preparationId2= UUID.randomUUID();
        Long userId = 1L;

        List<PreparationDto> mockPreparations = List.of(
                new PreparationDto(preparationId1, "가방 챙기기", 10, preparationId2),
                new PreparationDto(preparationId2, "화장", 5, null)
        );

        when(userAuthService.getUserIdFromToken(any(HttpServletRequest.class))).thenReturn(userId);
        when(scheduleService.getPreparations(userId, scheduleId)).thenReturn(mockPreparations);

        // when & then
        mockMvc.perform(get("/schedule/get/preparation/{scheduleId}", scheduleId)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // HTTP 200 확인
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].preparationName").value("가방 챙기기"))
                .andExpect(jsonPath("$.data[1].preparationName").value("화장"))
                .andDo(print());

        verify(userAuthService, times(1)).getUserIdFromToken(any(HttpServletRequest.class));
        verify(scheduleService, times(1)).getPreparations(userId, scheduleId);
    }

    @DisplayName("지각 히스토리 조회에 성공한다.")
    @Test
    void getLatenessHistory() throws Exception {
        // given
        List<LatenessHistoryResponse> latenessHistory = new ArrayList<>();
        latenessHistory.add(new LatenessHistoryResponse(
                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"),
                "을사년 새해",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                1
        ));

        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(scheduleService.getLatenessHistory(any(Long.class))).thenReturn(latenessHistory);

        // when // then
        mockMvc.perform(
                        get("/schedule/lateness-history")
                                .content("")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("지각 히스토리 조회 성공!"))
                .andExpect(jsonPath("$.data[0].scheduleId").value("3fa85f64-5717-4562-b3fc-2c963f66afe5"));
    }

    @DisplayName("약속 종료(성실도점수/지각시간 업데이트)에 성공한다.")
    @Test
    void finishSchedule() throws Exception {
        // given
        FinishPreparationDto finishPreparationDto = FinishPreparationDto.builder()
                .scheduleId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afe5"))
                .build();

        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        doNothing().when(scheduleService).finishSchedule(any(Long.class), any(FinishPreparationDto.class));

        // when // then
        mockMvc.perform(
                        put("/schedule/finish")
                                .content(objectMapper.writeValueAsString(finishPreparationDto))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("지각시간과 성실도점수가 성공적으로 업데이트 되었습니다!"));
    }
}