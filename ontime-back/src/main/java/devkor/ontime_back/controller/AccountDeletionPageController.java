package devkor.ontime_back.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
public class AccountDeletionPageController {

    @GetMapping(value = "/account-deletion", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getAccountDeletionPage() {
        return html(koreanAccountDeletionPage());
    }

    @GetMapping(value = "/account-deletion/en", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getEnglishAccountDeletionPage() {
        return html(englishAccountDeletionPage());
    }

    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(body);
    }

    private String koreanAccountDeletionPage() {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <link rel="canonical" href="https://ontime-back.duckdns.org/account-deletion">
                  <link rel="alternate" hreflang="en" href="https://ontime-back.duckdns.org/account-deletion/en">
                  <link rel="alternate" hreflang="ko" href="https://ontime-back.duckdns.org/account-deletion">
                  <title>OnTime 계정 삭제 요청</title>
                  <style>
                    :root {
                      color-scheme: light;
                      --ink: #18212f;
                      --muted: #5a6678;
                      --line: #d8e0ea;
                      --surface: #ffffff;
                      --page: #f4f7fb;
                      --accent: #1f7a5c;
                    }
                    * {
                      box-sizing: border-box;
                    }
                    body {
                      margin: 0;
                      background: var(--page);
                      color: var(--ink);
                      font-family: -apple-system, BlinkMacSystemFont, "Apple SD Gothic Neo", "Noto Sans KR", "Segoe UI", sans-serif;
                      line-height: 1.72;
                      word-break: keep-all;
                    }
                    main {
                      width: min(880px, calc(100% - 32px));
                      margin: 0 auto;
                      padding: 48px 0;
                    }
                    header {
                      border-bottom: 1px solid var(--line);
                      padding-bottom: 24px;
                      margin-bottom: 28px;
                    }
                    h1 {
                      font-size: clamp(2rem, 5vw, 3rem);
                      line-height: 1.15;
                      margin: 0 0 12px;
                    }
                    h2 {
                      font-size: 1.25rem;
                      margin: 32px 0 8px;
                    }
                    p, li {
                      font-size: 1rem;
                    }
                    a {
                      color: var(--accent);
                      font-weight: 700;
                    }
                    .panel {
                      background: var(--surface);
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      padding: 24px;
                      box-shadow: 0 8px 24px rgba(24, 33, 47, 0.06);
                    }
                    .meta {
                      color: var(--muted);
                      margin: 0;
                    }
                    .request {
                      border-left: 4px solid var(--accent);
                      padding-left: 16px;
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <header>
                      <h1>OnTime 계정 삭제 요청</h1>
                      <p class="meta">앱 이름: OnTime</p>
                      <p class="meta">개발자: ejun</p>
                      <p class="meta">문의 이메일: <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a></p>
                      <p class="meta">English: <a href="https://ontime-back.duckdns.org/account-deletion/en">Account deletion request</a></p>
                    </header>

                    <section class="panel">
                      <h2>앱 외부에서 계정 삭제 요청하기</h2>
                      <p class="request">
                        OnTime 앱을 설치하거나 열지 않아도
                        <a href="mailto:jjoonleo@gmail.com?subject=OnTime%20account%20deletion%20request">jjoonleo@gmail.com</a>으로
                        이메일을 보내 계정 삭제를 요청할 수 있습니다. OnTime 계정에 연결된 이메일 주소로 보내주시고,
                        제목에 "OnTime account deletion request"를 포함해 주세요.
                      </p>
                      <p>
                        OnTime은 요청자가 계정 소유자인지 확인하는 데 필요한 경우에만 추가 정보를 요청할 수 있습니다.
                        이메일로 삭제를 요청하기 위해 이 웹사이트에 로그인할 필요는 없습니다.
                      </p>

                      <h2>삭제되는 데이터</h2>
                      <p>
                        사용자가 OnTime 계정을 삭제하면 OnTime은 로컬 계정과 관련 앱 데이터를 삭제합니다.
                        여기에는 일정, 준비 데이터, 알림 일정, 사용자 설정, 알람 설정, 알람 상태,
                        기기 기록, FCM 토큰, 세션 토큰이 포함됩니다.
                      </p>

                      <h2>보관되는 데이터</h2>
                      <p>
                        사용자가 선택적으로 계정 삭제 피드백을 제출한 경우, OnTime은 서비스 품질 및 삭제 관련
                        지원 이슈를 검토하기 위해 해당 피드백을 최대 1년 동안 보관할 수 있습니다. 이 피드백은
                        삭제된 계정과 분리되어 저장되며, 일반 텍스트 이메일 주소 대신 해시 처리된 이메일 값을 사용합니다.
                      </p>
                      <p>
                        운영 로그, 모니터링 기록, 보안 기록은 서비스 운영, 디버깅, 보안 및 악용 방지를 위해
                        최대 90일 동안 보관될 수 있습니다. 법적 준수 또는 진행 중인 보안 조사에 더 긴 기간이
                        필요한 경우에는 예외적으로 더 오래 보관될 수 있습니다.
                      </p>
                      <p>
                        삭제된 계정 데이터가 포함된 백업 사본은 일반적인 백업 순환 주기에 따라 제거되며,
                        법률 또는 보안 조사상 더 긴 기간이 필요한 경우를 제외하고 30일을 초과하여 보관되지 않습니다.
                      </p>

                      <h2>개인정보 처리방침</h2>
                      <p>
                        OnTime의 공개 개인정보 처리방침은
                        <a href="https://ontime-back.duckdns.org/privacy-policy">https://ontime-back.duckdns.org/privacy-policy</a>에서
                        확인할 수 있습니다. 이 URL은 계정 삭제 요청 URL과 함께 Google Play Console에 등록할 수 있습니다.
                      </p>
                    </section>
                  </main>
                </body>
                </html>
                """;
    }

    private String englishAccountDeletionPage() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <link rel="canonical" href="https://ontime-back.duckdns.org/account-deletion/en">
                  <link rel="alternate" hreflang="ko" href="https://ontime-back.duckdns.org/account-deletion">
                  <title>OnTime Account Deletion Request</title>
                  <style>
                    :root {
                      color-scheme: light;
                      --ink: #18212f;
                      --muted: #5a6678;
                      --line: #d8e0ea;
                      --surface: #ffffff;
                      --page: #f4f7fb;
                      --accent: #1f7a5c;
                    }
                    * {
                      box-sizing: border-box;
                    }
                    body {
                      margin: 0;
                      background: var(--page);
                      color: var(--ink);
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                      line-height: 1.62;
                    }
                    main {
                      width: min(880px, calc(100% - 32px));
                      margin: 0 auto;
                      padding: 48px 0;
                    }
                    header {
                      border-bottom: 1px solid var(--line);
                      padding-bottom: 24px;
                      margin-bottom: 28px;
                    }
                    h1 {
                      font-size: clamp(2rem, 5vw, 3rem);
                      line-height: 1.1;
                      margin: 0 0 12px;
                    }
                    h2 {
                      font-size: 1.25rem;
                      margin: 32px 0 8px;
                    }
                    p, li {
                      font-size: 1rem;
                    }
                    a {
                      color: var(--accent);
                      font-weight: 700;
                    }
                    .panel {
                      background: var(--surface);
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      padding: 24px;
                      box-shadow: 0 8px 24px rgba(24, 33, 47, 0.06);
                    }
                    .meta {
                      color: var(--muted);
                      margin: 0;
                    }
                    .request {
                      border-left: 4px solid var(--accent);
                      padding-left: 16px;
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <header>
                      <h1>OnTime Account Deletion Request</h1>
                      <p class="meta">App name: OnTime</p>
                      <p class="meta">Developer: ejun</p>
                      <p class="meta">Contact email: <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a></p>
                      <p class="meta">한국어: <a href="https://ontime-back.duckdns.org/account-deletion">계정 삭제 요청</a></p>
                    </header>

                    <section class="panel">
                      <h2>Request Account Deletion Outside The App</h2>
                      <p class="request">
                        You can request deletion of your OnTime account without installing or opening the app by emailing
                        <a href="mailto:jjoonleo@gmail.com?subject=OnTime%20account%20deletion%20request">jjoonleo@gmail.com</a>.
                        Please use the email address associated with your OnTime account and include the subject
                        "OnTime account deletion request".
                      </p>
                      <p>
                        OnTime may ask for additional information only as needed to verify that the request is from the
                        account owner. You do not need to log in to this website to submit the request by email.
                      </p>

                      <h2>Data Deleted</h2>
                      <p>
                        When a user deletes their OnTime account, OnTime deletes the local account and associated app data,
                        including schedules, preparation data, notification schedules, user settings, alarm settings,
                        alarm status, device records, FCM tokens, and session tokens.
                      </p>

                      <h2>Data Retained</h2>
                      <p>
                        If the user submits optional account deletion feedback, OnTime may retain that feedback for up to
                        1 year to review service quality and deletion-related support issues. This feedback is stored
                        separately from the deleted account and uses a hashed email value instead of the plaintext email address.
                      </p>
                      <p>
                        Operational logs, monitoring records, and security records may be retained for up to 90 days for
                        service operation, debugging, security, and abuse-prevention purposes, unless a longer period is
                        required for legal compliance or an active security investigation.
                      </p>
                      <p>
                        Backup copies containing deleted account data are removed according to the normal backup rotation
                        and are retained for no longer than 30 days, unless a longer period is required by law or security investigation.
                      </p>

                      <h2>Privacy Policy</h2>
                      <p>
                        The public OnTime privacy policy is available at
                        <a href="https://ontime-back.duckdns.org/privacy-policy">https://ontime-back.duckdns.org/privacy-policy</a>.
                        It can be listed in Google Play Console together with this account deletion request URL.
                      </p>
                    </section>
                  </main>
                </body>
                </html>
                """;
    }
}
