package devkor.ontime_back.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
public class PrivacyPolicyController {

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPrivacyPolicy() {
        return html(koreanPrivacyPolicy());
    }

    @GetMapping(value = "/privacy-policy/en", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getEnglishPrivacyPolicy() {
        return html(englishPrivacyPolicy());
    }

    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(body);
    }

    private String koreanPrivacyPolicy() {
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="robots" content="index, follow">
                  <link rel="canonical" href="https://ontime-back.duckdns.org/privacy-policy">
                  <link rel="alternate" hreflang="en" href="https://ontime-back.duckdns.org/privacy-policy/en">
                  <link rel="alternate" hreflang="ko" href="https://ontime-back.duckdns.org/privacy-policy">
                  <title>OnTime 개인정보 처리방침</title>
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
                      width: min(920px, calc(100% - 32px));
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
                    table {
                      width: 100%;
                      border-collapse: collapse;
                      margin-top: 12px;
                    }
                    th, td {
                      border: 1px solid var(--line);
                      padding: 12px;
                      text-align: left;
                      vertical-align: top;
                    }
                    th {
                      background: #edf3f8;
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
                    @media (max-width: 720px) {
                      table, thead, tbody, tr, th, td {
                        display: block;
                      }
                      thead {
                        display: none;
                      }
                      tr {
                        border: 1px solid var(--line);
                        margin-bottom: 12px;
                      }
                      td {
                        border: 0;
                        border-top: 1px solid var(--line);
                      }
                      td:first-child {
                        border-top: 0;
                        font-weight: 700;
                        background: #edf3f8;
                      }
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <header>
                      <h1>OnTime 개인정보 처리방침</h1>
                      <p class="meta">앱 이름: OnTime</p>
                      <p class="meta">개발자/운영 주체: ejun</p>
                      <p class="meta">문의 이메일: <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a></p>
                      <p class="meta">시행일: 2026년 5월 10일</p>
                      <p class="meta">English: <a href="https://ontime-back.duckdns.org/privacy-policy/en">Privacy Policy</a></p>
                    </header>

                    <section class="panel">
                      <p>
                        OnTime은 ejun이 제공합니다. 본 개인정보 처리방침은 사용자가 OnTime 앱을 이용할 때
                        OnTime이 데이터를 수집, 이용, 공유, 보호, 보관 및 삭제하는 방식을 설명합니다.
                      </p>
                      <p>
                        개인정보 관련 문의 또는 요청은
                        <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a>으로 연락해 주세요.
                      </p>

                      <h2>OnTime이 수집하거나 접근하는 데이터</h2>
                      <p>
                        OnTime은 계정, 일정, 준비 알림, 알람, 알림 및 지원 기능을 제공하기 위해 다음 데이터를
                        수집하거나 접근합니다.
                      </p>
                      <table>
                        <thead>
                          <tr>
                            <th>데이터</th>
                            <th>예시</th>
                            <th>목적</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>계정 데이터</td>
                            <td>이메일 주소, 표시 이름, 이메일 가입용 비밀번호, Google 로그인 토큰, Apple identity token, Apple authorization code, 사용 가능한 경우 Apple이 제공한 이름 또는 이메일.</td>
                            <td>계정 생성 및 인증, 로그인 상태 유지, 소셜 로그인 지원, 프로필 정보 불러오기.</td>
                          </tr>
                          <tr>
                            <td>일정/준비 데이터</td>
                            <td>일정 이름과 시간, 장소 정보, 이동 시간, 여유 시간, 메모, 일정 상태, 지각 시간, 준비 단계, 준비 소요 시간, 단계 순서.</td>
                            <td>일정 및 준비 계획의 생성, 수정, 표시, 완료, 삭제.</td>
                          </tr>
                          <tr>
                            <td>알람/알림 데이터</td>
                            <td>알람 설정, 알림 권한 상태, 기기 ID, FCM 토큰, 플랫폼, 앱 버전, OS 버전, 지원 알람 제공자, 알람 상태 보고, 활성화 또는 건너뛴 일정 ID, 알람 실패 사유.</td>
                            <td>일정 리마인더 및 알람 알림 발송, 현재 기기 등록, 알람 복구, 알람 동작 범위 진단.</td>
                          </tr>
                          <tr>
                            <td>피드백</td>
                            <td>선택적인 계정 삭제 피드백 또는 기타 피드백 메시지.</td>
                            <td>사용자 피드백 및 계정 삭제 요청 처리.</td>
                          </tr>
                          <tr>
                            <td>로컬 앱 데이터</td>
                            <td>기기에 저장된 사용자, 일정, 장소, 준비, 알람, 토큰 캐시 데이터.</td>
                            <td>앱 상태를 로컬에서 유지하고 앱 동작 지원.</td>
                          </tr>
                          <tr>
                            <td>기술/진단 데이터</td>
                            <td>네트워크 요청 메타데이터, 서버 로그, 오류 메타데이터, 보안 관련 운영 기록.</td>
                            <td>서비스 운영, 보안, 디버깅 및 유지보수.</td>
                          </tr>
                        </tbody>
                      </table>
                      <p>
                        현재 Android 릴리스 매니페스트에서 OnTime은 위치, 연락처, 카메라, 마이크, 전화, SMS,
                        저장소, 캘린더, 주변 기기 또는 Bluetooth 권한에 대한 앱 소유 접근을 요청하지 않습니다.
                        OnTime은 일정 리마인더와 알람 기능을 제공하기 위해 알림, 정확한 알람, 전체 화면 인텐트,
                        부팅 완료, 진동, Firebase Messaging 및 네트워크 관련 권한을 사용합니다.
                      </p>

                      <h2>OnTime의 데이터 이용 방식</h2>
                      <p>OnTime은 수집한 데이터를 다음 목적으로 사용합니다.</p>
                      <ul>
                        <li>사용자 계정 생성, 인증 및 관리.</li>
                        <li>이메일/비밀번호, Google, Apple 로그인 지원.</li>
                        <li>일정 생성, 수정, 완료, 삭제 및 표시.</li>
                        <li>기본 준비 단계 및 일정별 준비 단계 생성과 수정.</li>
                        <li>일정 리마인더, 준비 알림 및 알람 알림 발송.</li>
                        <li>알람 및 알림 발송을 위한 현재 기기 등록과 등록 해제.</li>
                        <li>선택적 피드백 및 계정 삭제 피드백 처리.</li>
                        <li>보안 유지, 악용 방지, 장애 디버깅 및 서비스 운영.</li>
                      </ul>

                      <h2>제3자 서비스 및 처리자</h2>
                      <p>
                        OnTime은 핵심 앱 동작에 필요한 범위에서 제3자 서비스와 SDK를 사용합니다. 여기에는
                        Google 계정 인증을 위한 Google Sign-In, Apple 계정 인증을 위한 Apple Sign-In,
                        앱 초기화 및 푸시 알림 발송을 위한 Firebase Core와 Firebase Cloud Messaging,
                        계정, 일정, 준비, 알람, 알림, 피드백 및 삭제 요청 처리를 위한 OnTime 백엔드/API
                        인프라가 포함됩니다.
                      </p>

                      <h2>데이터 공유</h2>
                      <p>
                        OnTime은 앱 기능, 인증, 알림, 호스팅, 보안, 운영 및 지원을 제공하는 데 필요한 범위에서만
                        서비스 제공자와 데이터를 공유합니다. 현재 릴리스 빌드에서 OnTime은 앱 내 광고를 사용하지 않습니다.
                      </p>

                      <h2>안전한 데이터 처리</h2>
                      <p>
                        OnTime은 HTTPS API 통신, 토큰 기반 인증, 로컬 보안 토큰 저장, 릴리스 로그 제한,
                        비식별화 및 마스킹 관행을 통해 개인 및 민감한 데이터를 보호합니다. 릴리스 빌드는 토큰,
                        Authorization 헤더, 요청 본문, 응답 본문, 개인 일정 페이로드, 전체 알람 페이로드,
                        OAuth 값 또는 FCM 토큰을 로그로 남겨서는 안 됩니다.
                      </p>

                      <h2>데이터 보관</h2>
                      <p>
                        OnTime은 서비스를 제공하고, 보안을 유지하며, 법적 의무를 이행하고, 분쟁을 해결하며,
                        약관을 집행하는 데 필요한 기간 동안 계정, 일정, 준비, 알람, 알림, 피드백 및 기술 데이터를 보관합니다.
                      </p>
                      <p>
                        OnTime 계정이 삭제되면 계정 데이터와 사용자 소유 앱 데이터가 삭제됩니다. 여기에는 관련 일정,
                        준비 데이터, 알림 일정, 사용자 설정, 알람 설정, 알람 상태, 기기 기록, FCM 토큰,
                        세션 토큰이 포함됩니다.
                      </p>
                      <p>
                        사용자가 선택적으로 계정 삭제 피드백을 제출한 경우, OnTime은 서비스 품질 및 삭제 관련
                        지원 이슈를 검토하기 위해 해당 피드백을 최대 1년 동안 보관할 수 있습니다.
                      </p>
                      <p>
                        운영 로그, 모니터링 기록, 보안 기록은 서비스 운영, 디버깅, 보안 및 악용 방지를 위해
                        최대 90일 동안 보관될 수 있습니다. 법적 준수 또는 진행 중인 보안 조사에 더 긴 기간이
                        필요한 경우에는 예외적으로 더 오래 보관될 수 있습니다.
                      </p>
                      <p>
                        삭제된 계정 데이터가 포함된 백업 사본은 일반적인 백업 순환 주기에 따라 제거되며,
                        법률 또는 진행 중인 보안 조사상 더 긴 기간이 필요한 경우를 제외하고 30일을 초과하여
                        보관되지 않습니다.
                      </p>

                      <h2>계정 및 데이터 삭제</h2>
                      <p>
                        사용자는 OnTime 앱 안에서 계정 삭제를 요청할 수 있습니다. 삭제가 성공하면 앱은 사용자를 로그아웃합니다.
                      </p>
                      <p>
                        사용자는 앱 외부에서도
                        <a href="https://ontime-back.duckdns.org/account-deletion">https://ontime-back.duckdns.org/account-deletion</a>에서
                        계정 삭제를 요청할 수 있습니다.
                      </p>
                      <p>
                        Google 및 Apple 소셜 계정의 경우, 백엔드는 로컬 OnTime 계정을 삭제하기 전에 저장된
                        제공자 토큰 해지를 시도합니다. 제공자 토큰 해지가 실패하더라도 백엔드는 로컬 OnTime 계정을 삭제합니다.
                        OnTime 계정 삭제는 사용자의 Google 계정 또는 Apple ID를 삭제하지 않습니다.
                      </p>

                      <h2>아동</h2>
                      <p>
                        OnTime은 아동을 대상으로 하지 않습니다. 아동이 OnTime에 개인정보를 제공했다고 생각하는 경우
                        <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a>으로 연락해 주세요. 요청을 검토하겠습니다.
                      </p>

                      <h2>방침 변경</h2>
                      <p>
                        OnTime은 앱 동작, 법적 요구사항 또는 서비스 제공자의 변경을 반영하기 위해 본 개인정보 처리방침을
                        업데이트할 수 있습니다. 방침이 변경되면 위 시행일이 업데이트됩니다.
                      </p>
                    </section>
                  </main>
                </body>
                </html>
                """;
    }

    private String englishPrivacyPolicy() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="robots" content="index, follow">
                  <link rel="canonical" href="https://ontime-back.duckdns.org/privacy-policy/en">
                  <link rel="alternate" hreflang="ko" href="https://ontime-back.duckdns.org/privacy-policy">
                  <title>OnTime Privacy Policy</title>
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
                      width: min(920px, calc(100% - 32px));
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
                    table {
                      width: 100%;
                      border-collapse: collapse;
                      margin-top: 12px;
                    }
                    th, td {
                      border: 1px solid var(--line);
                      padding: 12px;
                      text-align: left;
                      vertical-align: top;
                    }
                    th {
                      background: #edf3f8;
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
                    @media (max-width: 720px) {
                      table, thead, tbody, tr, th, td {
                        display: block;
                      }
                      thead {
                        display: none;
                      }
                      tr {
                        border: 1px solid var(--line);
                        margin-bottom: 12px;
                      }
                      td {
                        border: 0;
                        border-top: 1px solid var(--line);
                      }
                      td:first-child {
                        border-top: 0;
                        font-weight: 700;
                        background: #edf3f8;
                      }
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <header>
                      <h1>OnTime Privacy Policy</h1>
                      <p class="meta">App name: OnTime</p>
                      <p class="meta">Developer/entity: ejun</p>
                      <p class="meta">Contact email: <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a></p>
                      <p class="meta">Effective date: May 10, 2026</p>
                      <p class="meta">한국어: <a href="https://ontime-back.duckdns.org/privacy-policy">개인정보 처리방침</a></p>
                    </header>

                    <section class="panel">
                      <p>
                        OnTime is provided by ejun. This Privacy Policy explains how OnTime collects, uses,
                        shares, protects, retains, and deletes data when you use the OnTime app.
                      </p>
                      <p>
                        For privacy questions or requests, contact
                        <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a>.
                      </p>

                      <h2>Data OnTime Collects Or Accesses</h2>
                      <p>
                        OnTime collects or accesses the following data to provide accounts, schedules,
                        preparation reminders, alarms, notifications, and support features.
                      </p>
                      <table>
                        <thead>
                          <tr>
                            <th>Data</th>
                            <th>Examples</th>
                            <th>Purpose</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr>
                            <td>Account data</td>
                            <td>Email address, display name, password for email sign-up, Google sign-in token, Apple identity token, Apple authorization code, and Apple-provided name or email when available.</td>
                            <td>Create and authenticate accounts, keep users signed in, support social sign-in, and load profile information.</td>
                          </tr>
                          <tr>
                            <td>Schedule/preparation data</td>
                            <td>Schedule names and times, place information, movement time, spare time, notes, schedule state, lateness time, preparation steps, preparation durations, and step order.</td>
                            <td>Create, update, display, finish, and delete schedules and preparation plans.</td>
                          </tr>
                          <tr>
                            <td>Alarm/notification data</td>
                            <td>Alarm settings, notification permission state, device ID, FCM token, platform, app version, OS version, supported alarm providers, alarm status reports, armed or skipped schedule IDs, and alarm failure reason.</td>
                            <td>Deliver schedule reminders and alarm notifications, register the current device, restore alarms, and diagnose alarm coverage.</td>
                          </tr>
                          <tr>
                            <td>Feedback</td>
                            <td>Optional account deletion feedback or other feedback messages.</td>
                            <td>Process user feedback and account deletion requests.</td>
                          </tr>
                          <tr>
                            <td>Local app data</td>
                            <td>Cached user, schedule, place, preparation, alarm, and token data stored on the device.</td>
                            <td>Keep app state available locally and support app operation.</td>
                          </tr>
                          <tr>
                            <td>Technical/diagnostic data</td>
                            <td>Network request metadata, server logs, error metadata, and security-related operational records.</td>
                            <td>Operate, secure, debug, and maintain the service.</td>
                          </tr>
                        </tbody>
                      </table>
                      <p>
                        OnTime does not request app-owned access to location, contacts, camera, microphone,
                        phone, SMS, storage, calendar, nearby-device, or Bluetooth permissions in the current
                        Android release manifest. OnTime uses notification, exact alarm, full-screen intent,
                        boot completion, vibration, Firebase messaging, and network-related permissions to
                        provide schedule reminders and alarm functionality.
                      </p>

                      <h2>How OnTime Uses Data</h2>
                      <p>OnTime uses collected data to:</p>
                      <ul>
                        <li>Create, authenticate, and manage user accounts.</li>
                        <li>Support email/password, Google, and Apple sign-in.</li>
                        <li>Create, update, finish, delete, and display schedules.</li>
                        <li>Create and update default and schedule-specific preparation steps.</li>
                        <li>Send schedule reminders, preparation notifications, and alarm notifications.</li>
                        <li>Register and unregister the current device for alarm and notification delivery.</li>
                        <li>Process optional feedback and account deletion feedback.</li>
                        <li>Maintain security, prevent abuse, debug failures, and operate the service.</li>
                      </ul>

                      <h2>Third-Party Services And Processors</h2>
                      <p>
                        OnTime uses third-party services and SDKs where needed for core app behavior, including
                        Google Sign-In for Google account authentication, Apple Sign-In for Apple account
                        authentication, Firebase Core and Firebase Cloud Messaging for app initialization and
                        push notification delivery, and OnTime backend/API infrastructure for account,
                        schedule, preparation, alarm, notification, feedback, and deletion request processing.
                      </p>

                      <h2>Data Sharing</h2>
                      <p>
                        OnTime shares data with service providers only as needed to provide app functionality,
                        authentication, notifications, hosting, security, operations, and support. OnTime does
                        not use in-app advertising in the current release build.
                      </p>

                      <h2>Secure Data Handling</h2>
                      <p>
                        OnTime uses HTTPS API communication, token-based authentication, local secure token
                        storage, release-log restrictions, and redaction practices to protect personal and
                        sensitive data. Release builds must not log tokens, authorization headers, request
                        bodies, response bodies, personal schedule payloads, full alarm payloads, OAuth values,
                        or FCM tokens.
                      </p>

                      <h2>Data Retention</h2>
                      <p>
                        OnTime keeps account, schedule, preparation, alarm, notification, feedback, and
                        technical data for as long as needed to provide the service, maintain security, meet
                        legal obligations, resolve disputes, and enforce agreements.
                      </p>
                      <p>
                        When an OnTime account is deleted, account data and user-owned app data are deleted,
                        including associated schedules, preparation data, notification schedules, user settings,
                        alarm settings, alarm status, device records, FCM tokens, and session tokens.
                      </p>
                      <p>
                        If a user submits optional account deletion feedback, OnTime may retain that feedback
                        for up to 1 year to review service quality and deletion-related support issues.
                      </p>
                      <p>
                        Operational logs, monitoring records, and security records may be retained for up to 90
                        days for service operation, debugging, security, and abuse-prevention purposes, unless a
                        longer period is required for legal compliance or an active security investigation.
                      </p>
                      <p>
                        Backup copies that contain deleted account data are removed according to the normal
                        backup rotation and are retained for no longer than 30 days, unless a longer period is
                        required by law or an active security investigation.
                      </p>

                      <h2>Account And Data Deletion</h2>
                      <p>
                        Users can request account deletion from within the OnTime app. On successful deletion,
                        the app signs the user out.
                      </p>
                      <p>
                        Users can also request account deletion outside the app at
                        <a href="https://ontime-back.duckdns.org/account-deletion">https://ontime-back.duckdns.org/account-deletion</a>.
                      </p>
                      <p>
                        For Google and Apple social accounts, the backend attempts to revoke the stored provider
                        token before deleting the local OnTime account. If provider token revocation fails, the
                        backend still deletes the local OnTime account. Deleting an OnTime account does not
                        delete the user's Google account or Apple ID.
                      </p>

                      <h2>Children</h2>
                      <p>
                        OnTime is not directed to children. If you believe a child has provided personal data to
                        OnTime, contact <a href="mailto:jjoonleo@gmail.com">jjoonleo@gmail.com</a> so the
                        request can be reviewed.
                      </p>

                      <h2>Changes To This Policy</h2>
                      <p>
                        OnTime may update this Privacy Policy to reflect changes in app behavior, legal
                        requirements, or service providers. The effective date above will be updated when the
                        policy changes.
                      </p>
                    </section>
                  </main>
                </body>
                </html>
                """;
    }
}
