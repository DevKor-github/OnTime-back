package devkor.ontime_back.controller;

import devkor.ontime_back.ControllerTestSupport;
import devkor.ontime_back.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
class PrivacyPolicyControllerTest extends ControllerTestSupport {

    @DisplayName("개인정보 처리방침 페이지를 로그인 없이 HTML로 조회한다.")
    @Test
    void getPrivacyPolicy() throws Exception {
        mockMvc.perform(get("/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("OnTime Privacy Policy")))
                .andExpect(content().string(containsString("App name: OnTime")))
                .andExpect(content().string(containsString("Developer/entity: ejun")))
                .andExpect(content().string(containsString("jjoonleo@gmail.com")))
                .andExpect(content().string(containsString("Effective date: May 10, 2026")))
                .andExpect(content().string(containsString("https://ontime-back.duckdns.org/account-deletion")))
                .andExpect(content().string(containsString("Account data")))
                .andExpect(content().string(containsString("Schedule/preparation data")))
                .andExpect(content().string(containsString("Alarm/notification data")))
                .andExpect(content().string(containsString("Feedback")))
                .andExpect(content().string(containsString("Local app data")))
                .andExpect(content().string(containsString("Technical/diagnostic data")))
                .andExpect(content().string(containsString("account data and user-owned app data are deleted")))
                .andExpect(content().string(containsString("for up to 1 year")))
                .andExpect(content().string(containsString("for up to 90")))
                .andExpect(content().string(containsString("retained for no longer than 30 days")));
    }
}
