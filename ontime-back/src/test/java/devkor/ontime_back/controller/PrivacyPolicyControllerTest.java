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
                .andExpect(content().string(containsString("<html lang=\"ko\">")))
                .andExpect(content().string(containsString("OnTime 개인정보 처리방침")))
                .andExpect(content().string(containsString("앱 이름: OnTime")))
                .andExpect(content().string(containsString("개발자/운영 주체: ejun")))
                .andExpect(content().string(containsString("문의 이메일")))
                .andExpect(content().string(containsString("시행일: 2026년 5월 10일")))
                .andExpect(content().string(containsString("https://ontime-back.duckdns.org/account-deletion")))
                .andExpect(content().string(containsString("계정 데이터")))
                .andExpect(content().string(containsString("일정/준비 데이터")))
                .andExpect(content().string(containsString("알람/알림 데이터")))
                .andExpect(content().string(containsString("피드백")))
                .andExpect(content().string(containsString("로컬 앱 데이터")))
                .andExpect(content().string(containsString("기술/진단 데이터")))
                .andExpect(content().string(containsString("계정 데이터와 사용자 소유 앱 데이터가 삭제됩니다")))
                .andExpect(content().string(containsString("최대 1년")))
                .andExpect(content().string(containsString("최대 90일")))
                .andExpect(content().string(containsString("30일을 초과하여")));
    }

    @DisplayName("영문 개인정보 처리방침 페이지를 로그인 없이 HTML로 조회한다.")
    @Test
    void getEnglishPrivacyPolicy() throws Exception {
        mockMvc.perform(get("/privacy-policy/en"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<html lang=\"en\">")))
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
