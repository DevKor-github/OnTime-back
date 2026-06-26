package devkor.ontime_back.controller;

import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.dto.AlarmDeviceCurrentRequestDto;
import devkor.ontime_back.dto.AlarmDeviceCurrentResponseDto;
import devkor.ontime_back.dto.AlarmDeviceUnregisterRequestDto;
import devkor.ontime_back.dto.AlarmDeviceUnregisterResponseDto;
import devkor.ontime_back.dto.AlarmSettingsResponseDto;
import devkor.ontime_back.dto.AlarmSettingsPatchDto;
import devkor.ontime_back.dto.AlarmStatusCurrentResponseDto;
import devkor.ontime_back.dto.AlarmStatusReportRequestDto;
import devkor.ontime_back.dto.AlarmStatusReportResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class AlarmControllerApiContractTest extends ControllerTestSupport {

    @Test
    void getAlarmSettingsReturnsCurrentUsersSettingsEnvelope() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(alarmService.getAlarmSettings(1L)).thenReturn(AlarmSettingsResponseDto.builder()
                .alarmsEnabled(true)
                .defaultAlarmOffsetMinutes(10)
                .updatedAt(Instant.parse("2026-05-05T00:00:00Z"))
                .build());

        mockMvc.perform(get("/users/me/alarm-settings")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.alarmsEnabled").value(true))
                .andExpect(jsonPath("$.data.defaultAlarmOffsetMinutes").value(10));
    }

    @Test
    void patchAlarmSettingsBindsPartialJsonAndReturnsUpdatedSettings() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(alarmService.patchAlarmSettings(eq(1L), any(AlarmSettingsPatchDto.class))).thenReturn(AlarmSettingsResponseDto.builder()
                .alarmsEnabled(false)
                .defaultAlarmOffsetMinutes(30)
                .updatedAt(Instant.parse("2026-05-05T00:00:00Z"))
                .build());

        mockMvc.perform(patch("/users/me/alarm-settings")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alarmsEnabled\":false,\"defaultAlarmOffsetMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alarmsEnabled").value(false))
                .andExpect(jsonPath("$.data.defaultAlarmOffsetMinutes").value(30));
    }

    @Test
    void registerCurrentDevicePassesSessionTokensAndReturnsDeviceBinding() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(userAuthService.getAccessTokenFromRequest(any())).thenReturn("access-token");
        when(userAuthService.getRefreshTokenFromRequest(any())).thenReturn("refresh-token");
        when(alarmService.registerCurrentDevice(eq(1L), any(AlarmDeviceCurrentRequestDto.class), eq("access-token"), eq("refresh-token")))
                .thenReturn(AlarmDeviceCurrentResponseDto.builder()
                        .deviceId("ios-device-000001")
                        .active(true)
                        .lastSeenAt(Instant.parse("2026-05-05T00:00:00Z"))
                        .build());

        mockMvc.perform(put("/users/me/devices/current")
                        .header("Authorization", "Bearer access-token")
                        .header("Authorization-refresh", "Bearer refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "ios-device-000001",
                                  "platform": "ios",
                                  "appVersion": "1.2.3",
                                  "osVersion": "18.0",
                                  "supportsNativeAlarm": true,
                                  "nativeAlarmProvider": "iosAlarmKit",
                                  "fallbackProvider": "localNotification"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceId").value("ios-device-000001"))
                .andExpect(jsonPath("$.data.active").value(true));

        verify(alarmService).registerCurrentDevice(eq(1L), any(AlarmDeviceCurrentRequestDto.class), eq("access-token"), eq("refresh-token"));
    }

    @Test
    void unregisterCurrentDeviceAcceptsOptionalDeviceIdAndReturnsInactiveBinding() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(userAuthService.getAccessTokenFromRequest(any())).thenReturn("access-token");
        when(alarmService.unregisterCurrentDevice(eq(1L), any(AlarmDeviceUnregisterRequestDto.class), eq("access-token")))
                .thenReturn(AlarmDeviceUnregisterResponseDto.builder()
                        .active(false)
                        .build());

        mockMvc.perform(delete("/users/me/devices/current")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"ios-device-000001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void reportAlarmStatusBindsReconciliationPayloadAndReturnsReceipt() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(userAuthService.getAccessTokenFromRequest(any())).thenReturn("access-token");
        when(alarmService.reportAlarmStatus(eq(1L), any(AlarmStatusReportRequestDto.class), eq("access-token")))
                .thenReturn(AlarmStatusReportResponseDto.builder().received(true).build());

        mockMvc.perform(post("/users/me/alarm-status")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "ios-device-000001",
                                  "reconciledAt": "2026-05-05T09:00:00+09:00",
                                  "scheduleWindowStart": "2026-05-05T00:00:00",
                                  "scheduleWindowEnd": "2026-05-06T00:00:00",
                                  "alarmCoverageStart": "2026-05-05T00:00:00",
                                  "alarmCoverageEnd": "2026-05-06T00:00:00",
                                  "status": "armed",
                                  "nativeAlarmProvider": "iosAlarmKit",
                                  "fallbackProvider": "localNotification",
                                  "armedScheduleCount": 1,
                                  "armedScheduleIds": ["schedule-1"],
                                  "skippedScheduleCount": 0,
                                  "failures": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.received").value(true));
    }

    @Test
    void getCurrentAlarmStatusReturnsCurrentDeviceStatusEnvelope() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(userAuthService.getAccessTokenFromRequest(any())).thenReturn("access-token");
        when(alarmService.getCurrentAlarmStatus(1L, "access-token")).thenReturn(AlarmStatusCurrentResponseDto.builder()
                .deviceId("ios-device-000001")
                .active(true)
                .platform("ios")
                .reconciledAt(Instant.parse("2026-05-05T00:00:00Z"))
                .scheduleWindowStart(LocalDateTime.of(2026, 5, 5, 0, 0))
                .scheduleWindowEnd(LocalDateTime.of(2026, 5, 6, 0, 0))
                .status("armed")
                .armedScheduleIds(List.of("schedule-1"))
                .updatedAt(Instant.parse("2026-05-05T00:00:00Z"))
                .build());

        mockMvc.perform(get("/users/me/alarm-status")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceId").value("ios-device-000001"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.status").value("armed"));
    }
}
