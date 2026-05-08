package devkor.ontime_back.controller;

import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class RequestValidationControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("회원가입 요청의 이메일과 약한 비밀번호를 검증한다")
    void signUpValidationFailure() throws Exception {
        mockMvc.perform(post("/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "bad-email",
                                "password", "password123",
                                "name", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verify(userAuthService, never()).signUp(any(), any(), any());
    }

    @Test
    @DisplayName("일정 추가 요청의 음수 시간과 과거 시간을 검증한다")
    void addScheduleValidationFailure() throws Exception {
        mockMvc.perform(post("/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduleId": "%s",
                                  "placeId": "%s",
                                  "placeName": "",
                                  "scheduleName": "",
                                  "moveTime": -1,
                                  "scheduleTime": "2020-01-01T09:00:00",
                                  "scheduleSpareTime": -1,
                                  "scheduleNote": "note"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(scheduleService);
    }

    @Test
    @DisplayName("준비과정 목록 요청은 비어 있거나 잘못된 항목이면 거절한다")
    void preparationListValidationFailure() throws Exception {
        mockMvc.perform(put("/users/preparations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(preparationUserService);
    }

    @Test
    @DisplayName("친구 요청 수락 상태는 허용된 값만 받는다")
    void friendshipAcceptStatusValidationFailure() throws Exception {
        mockMvc.perform(post("/friends/{uuid}/approve", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptStatus\":\"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(friendshipService);
    }

    @Test
    @DisplayName("사용자 여유시간과 앱 설정 범위를 검증한다")
    void userSettingValidationFailure() throws Exception {
        mockMvc.perform(put("/users/me/spare-time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newSpareTime\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        mockMvc.perform(put("/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"soundVolume\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(userService);
        verifyNoInteractions(userSettingService);
    }

    @Test
    @DisplayName("피드백과 탈퇴 피드백의 길이를 검증한다")
    void feedbackValidationFailure() throws Exception {
        String oversizedMessage = "x".repeat(1001);

        mockMvc.perform(post("/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", oversizedMessage))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        mockMvc.perform(delete("/oauth2/google/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", oversizedMessage))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(feedbackService);
        verify(userAuthService, never()).deleteUser(any(), any());
    }

    @Test
    @DisplayName("FCM 토큰과 디바이스 ID를 검증한다")
    void firebaseTokenValidationFailure() throws Exception {
        mockMvc.perform(post("/firebase-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firebaseToken\":\"\",\"deviceId\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(firebaseTokenService);
    }

    @Test
    @DisplayName("알람 설정 패치의 알 수 없는 필드와 타입을 검증한다")
    void alarmSettingsValidationFailure() throws Exception {
        mockMvc.perform(patch("/users/me/alarm-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alarmsEnabled\":\"true\",\"unknown\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(alarmService);
    }

    @Test
    @DisplayName("알람 상태 보고 요청의 날짜 범위와 enum 값을 검증한다")
    void alarmStatusValidationFailure() throws Exception {
        mockMvc.perform(post("/users/me/alarm-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "ios-device-000001",
                                  "reconciledAt": "%s",
                                  "scheduleWindowStart": "2026-05-08T10:00:00",
                                  "scheduleWindowEnd": "2026-05-08T09:00:00",
                                  "alarmCoverageStart": "2026-05-08T00:00:00",
                                  "alarmCoverageEnd": "2026-05-08T01:00:00",
                                  "status": "bad",
                                  "nativeAlarmProvider": "iosAlarmKit",
                                  "fallbackProvider": "localNotification"
                                }
                                """.formatted(LocalDateTime.now().toString() + "+09:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.data.errors").isArray());

        verifyNoInteractions(alarmService);
    }
}
