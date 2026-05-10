package devkor.ontime_back.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class AccountDeletionPageController {

    @GetMapping(value = "/account-deletion", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getAccountDeletionPage() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body("""
                        <!doctype html>
                        <html lang="en">
                        <head>
                          <meta charset="utf-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1">
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
                                Once the public OnTime privacy policy is hosted, it should be linked from this page and listed in
                                Google Play Console together with this account deletion request URL.
                              </p>
                            </section>
                          </main>
                        </body>
                        </html>
                        """);
    }
}
