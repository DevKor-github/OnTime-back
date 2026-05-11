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
class AccountDeletionPageControllerTest extends ControllerTestSupport {

    @DisplayName("계정 삭제 요청 페이지를 로그인 없이 HTML로 조회한다.")
    @Test
    void getAccountDeletionPage() throws Exception {
        mockMvc.perform(get("/account-deletion"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<html lang=\"ko\">")))
                .andExpect(content().string(containsString("OnTime 계정 삭제 요청")))
                .andExpect(content().string(containsString("앱 이름: OnTime")))
                .andExpect(content().string(containsString("개발자: ejun")))
                .andExpect(content().string(containsString("문의 이메일")))
                .andExpect(content().string(containsString("앱을 설치하거나 열지 않아도")))
                .andExpect(content().string(containsString("일정, 준비 데이터, 알림 일정")))
                .andExpect(content().string(containsString("최대 1년 동안 보관")))
                .andExpect(content().string(containsString("최대 90일 동안 보관")))
                .andExpect(content().string(containsString("30일을 초과하여 보관되지 않습니다")))
                .andExpect(content().string(containsString("https://ontime-back.duckdns.org/privacy-policy")));
    }

    @DisplayName("영문 계정 삭제 요청 페이지를 로그인 없이 HTML로 조회한다.")
    @Test
    void getEnglishAccountDeletionPage() throws Exception {
        mockMvc.perform(get("/account-deletion/en"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("OnTime Account Deletion Request")))
                .andExpect(content().string(containsString("App name: OnTime")))
                .andExpect(content().string(containsString("Developer: ejun")))
                .andExpect(content().string(containsString("jjoonleo@gmail.com")))
                .andExpect(content().string(containsString("without installing or opening the app")))
                .andExpect(content().string(containsString("schedules, preparation data, notification schedules")))
                .andExpect(content().string(containsString("retain that feedback for up to")))
                .andExpect(content().string(containsString("Operational logs, monitoring records, and security records may be retained for up to 90 days")))
                .andExpect(content().string(containsString("retained for no longer than 30 days")))
                .andExpect(content().string(containsString("https://ontime-back.duckdns.org/privacy-policy")));
    }
}
