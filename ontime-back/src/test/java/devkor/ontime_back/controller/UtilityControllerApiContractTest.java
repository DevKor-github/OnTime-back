package devkor.ontime_back.controller;

import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class UtilityControllerApiContractTest extends ControllerTestSupport {

    @Test
    void firebaseTokenEndpointsExtractUserAndAccessToken() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        when(userAuthService.getAccessTokenFromRequest(any())).thenReturn("access-token");
        doNothing().when(firebaseTokenService).registerFirebaseToken(eq(1L), any(), eq("access-token"));

        mockMvc.perform(post("/firebase-token")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firebaseToken\":\"firebase-token\",\"deviceId\":\"ios-device-000001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FCM 토큰이 성공적으로 User테이블에 저장되었습니다!"));

        mockMvc.perform(post("/firebase-token/push-test")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Firebase 푸시 메세지가 성공적으로 Firebase에 전달되었습니다!"));

        verify(firebaseTokenService).sendTestNotification(1L);
    }

    @Test
    void userSettingEndpointsUpdateAndResetCurrentUsersSettings() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);

        mockMvc.perform(put("/users/me/settings")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isNotificationsEnabled": true,
                                  "soundVolume": 75,
                                  "isPlayOnSpeaker": false,
                                  "is24HourFormat": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("사용자 앱 설정이 성공적으로 업데이트되었습니다!"));

        mockMvc.perform(put("/users/me/settings/reset")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("사용자 앱 설정이 성공적으로 초기화되었습니다! (soundVolume 50, 나머지 모두 true)"));

        verify(userSettingService).resetSetting(1L);
    }

    @Test
    void feedbackEndpointBindsFeedbackPayloadForCurrentUser() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);

        mockMvc.perform(post("/feedback")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedbackId":"%s","message":"앱 사용 피드백입니다."}
                                """.formatted(feedbackId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("피드백이 성공적으로 저장되었습니다!"));

        verify(feedbackService).saveFeedback(eq(1L), any());
    }

    @Test
    void documentEndpointsReturnStaticLegalAndProductContent() throws Exception {
        mockMvc.perform(get("/documents/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이용약관 조회 성공"));

        mockMvc.perform(get("/documents/privacy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("개인정보처리방침 조회 성공"));

        mockMvc.perform(get("/documents/ontime-description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("온타임 소개글 조회 성공"));
    }

    @Test
    void socialLogoutContinuesAccountDeletionWhenProviderRevokeFails() throws Exception {
        when(userAuthService.getUserIdFromToken(any())).thenReturn(1L);
        org.mockito.Mockito.doThrow(new RuntimeException("provider down"))
                .when(googleLoginService).revokeToken(1L);
        org.mockito.Mockito.doThrow(new RuntimeException("provider down"))
                .when(appleLoginService).revokeToken(1L);

        mockMvc.perform(delete("/oauth2/google/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"bye\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("구글 로그인 회원탈퇴 성공"));

        mockMvc.perform(delete("/oauth2/apple/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"bye\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("애플 로그인 회원탈퇴 성공"));

        verify(userAuthService, org.mockito.Mockito.times(2)).deleteUser(eq(1L), any());
    }
}
