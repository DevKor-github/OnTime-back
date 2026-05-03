package devkor.ontime_back.controller;

import devkor.ontime_back.TestSecurityConfig;
import devkor.ontime_back.global.oauth.apple.AppleLoginService;
import devkor.ontime_back.global.oauth.google.GoogleLoginService;
import devkor.ontime_back.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SocialAuthController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "feature.apple-login.enabled=false")
class SocialAuthControllerAppleDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAuthService userAuthService;

    @MockBean
    private AppleLoginService appleLoginService;

    @MockBean
    private GoogleLoginService googleLoginService;

    @Test
    void appleDeleteUser_whenAppleLoginDisabled_returns503_andDoesNotRevoke() throws Exception {
        mockMvc.perform(delete("/oauth2/apple/me"))
                .andExpect(status().isServiceUnavailable());

        verify(appleLoginService, never()).revokeToken(anyLong());
        verify(userAuthService, never()).deleteUser(anyLong());
    }

    @Test
    void appleLogin_whenAppleLoginDisabled_returns503() throws Exception {
        mockMvc.perform(post("/oauth2/apple/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isServiceUnavailable());
    }
}
