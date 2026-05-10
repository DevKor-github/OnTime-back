package devkor.ontime_back.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class PrivacyPolicyController {

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPrivacyPolicy() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body("""
                        <!doctype html>
                        <html lang="en">
                        <head>
                          <meta charset="utf-8">
                          <meta name="viewport" content="width=device-width, initial-scale=1">
                          <meta name="robots" content="index, follow">
                          <link rel="canonical" href="https://ontime-back.duckdns.org/privacy-policy">
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
                        """);
    }
}
