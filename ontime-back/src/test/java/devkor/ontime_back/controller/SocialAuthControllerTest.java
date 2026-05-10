package devkor.ontime_back.controller;

import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class SocialAuthControllerTest extends ControllerTestSupport {

    @DisplayName("구글 토큰 철회가 실패해도 계정 삭제를 계속 진행한다")
    @Test
    void googleDeleteContinuesWhenTokenRevocationFails() throws Exception {
        // given
        Long userId = 1L;
        when(userAuthService.getUserIdFromToken(any())).thenReturn(userId);
        doThrow(new RuntimeException("revocation failed"))
                .when(googleLoginService)
                .revokeToken(userId);
        when(userAuthService.deleteUser(eq(userId), isNull())).thenReturn(userId);

        // when // then
        mockMvc.perform(delete("/oauth2/google/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("구글 로그인 회원탈퇴 성공"));

        verify(googleLoginService).revokeToken(userId);
        verify(userAuthService).deleteUser(eq(userId), isNull());
    }

    @DisplayName("애플 토큰 철회가 실패해도 계정 삭제를 계속 진행한다")
    @Test
    void appleDeleteContinuesWhenTokenRevocationFails() throws Exception {
        // given
        Long userId = 1L;
        when(userAuthService.getUserIdFromToken(any())).thenReturn(userId);
        doThrow(new RuntimeException("revocation failed"))
                .when(appleLoginService)
                .revokeToken(userId);
        when(userAuthService.deleteUser(eq(userId), isNull())).thenReturn(userId);

        // when // then
        mockMvc.perform(delete("/oauth2/apple/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("애플 로그인 회원탈퇴 성공"));

        verify(appleLoginService).revokeToken(userId);
        verify(userAuthService).deleteUser(eq(userId), isNull());
    }

    @DisplayName("카카오 전용 계정 삭제 엔드포인트는 제공하지 않는다")
    @Test
    void kakaoProviderSpecificDeleteEndpointIsNotMapped() throws Exception {
        mockMvc.perform(delete("/oauth2/kakao/me"))
                .andExpect(status().isNotFound());
    }
}
