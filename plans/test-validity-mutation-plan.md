# Test Validity Mutation Plan

## Goal
Prove that the new coverage-raising tests protect real behavior by intentionally breaking the behavior locally and confirming the relevant tests fail for the expected reason.

This is a validation plan only. Every mutation is temporary and must be reverted immediately after the expected failure is observed.

## Context
- The project is the Spring/Gradle backend under `ontime-back/`.
- The coverage gate is JaCoCo instruction coverage `>= 80%`.
- The current full verification command is:

```bash
cd ontime-back
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew clean check
```

- The highest-risk behaviors to validate are:
  - OAuth register access tokens must use the persisted `savedUser.getId()`, not the unsaved `newUser.getId()`.
  - Alarm status validation must reject invalid windows, permission issues, failure reasons, and unsupported fallback combinations.
  - Legacy reminder suppression must ignore stale native alarm reconciliation status.
- Current relevant files:
  - `ontime-back/src/main/java/devkor/ontime_back/global/oauth/google/GoogleLoginService.java`
  - `ontime-back/src/main/java/devkor/ontime_back/global/oauth/apple/AppleLoginService.java`
  - `ontime-back/src/main/java/devkor/ontime_back/global/oauth/kakao/KakaoLoginFilter.java`
  - `ontime-back/src/main/java/devkor/ontime_back/service/AlarmService.java`
  - `ontime-back/src/test/java/devkor/ontime_back/global/oauth/google/GoogleLoginServiceTest.java`
  - `ontime-back/src/test/java/devkor/ontime_back/global/oauth/apple/AppleLoginServiceTest.java`
  - `ontime-back/src/test/java/devkor/ontime_back/global/oauth/OAuthLoginFilterValidationTest.java`
  - `ontime-back/src/test/java/devkor/ontime_back/service/AlarmServiceTest.java`

## Decisions
- Use mutation testing manually, not a mutation-testing framework, because the target is a small set of suspicious behaviors and we need readable proof.
- Mutate production code only, not tests. If a test still passes after the mutation, the test is weak or not covering the intended behavior.
- Run the narrowest focused test command after each mutation. Run full `clean check` only after all mutations are reverted.
- Treat any failure outside the intended focused test as noise unless it also directly proves the mutated behavior.
- Do not keep mutation commits. Mutations are local probes only.

## Steps

### 1. Capture Baseline State
1. Confirm the current dirty state so mutation changes are not confused with real work:

```bash
git status --short
```

2. Run the full verification once:

```bash
cd ontime-back
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew clean check
```

3. Record baseline coverage from `build/reports/jacoco/test/jacocoTestReport.xml`.

Expected baseline:
- `BUILD SUCCESSFUL`
- Instruction coverage stays above `80%`.

### 2. Mutation Safety Protocol
For every mutation below:

1. Apply only one mutation at a time.
2. Run the listed focused test command.
3. Confirm the expected test fails.
4. Capture the failure reason in notes.
5. Revert the mutation before starting the next mutation.
6. Rerun the same focused test to confirm it passes again.

Recommended revert workflow:

```bash
git diff -- <mutated-file>
```

Then manually revert the small mutation hunk, or save the hunk and reverse-apply it. Do not revert unrelated changes.

### 3. OAuth Register Token ID Mutations

#### 3.1 Google Register Uses Unsaved User ID
Temporary mutation:
- In `GoogleLoginService.handleRegister`, replace:

```java
jwtTokenProvider.createAccessToken(savedUser.getEmail(), savedUser.getId())
```

with:

```java
jwtTokenProvider.createAccessToken(newUser.getEmail(), newUser.getId())
```

Run:

```bash
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew test --tests devkor.ontime_back.global.oauth.google.GoogleLoginServiceTest
```

Expected failure:
- `handleRegisterCreatesGuestUserSettingsAndDefaultAlarmSettings` fails because the test expects `createAccessToken("new@example.com", 2L)`.
- If it passes, the test is not protecting the persisted-ID token contract.

Revert the mutation and rerun the same command. It must pass.

#### 3.2 Apple Register Uses Unsaved User ID
Temporary mutation:
- In `AppleLoginService.handleRegister`, replace:

```java
jwtTokenProvider.createAccessToken(savedUser.getEmail(), savedUser.getId())
```

with:

```java
jwtTokenProvider.createAccessToken(newUser.getEmail(), newUser.getId())
```

Run:

```bash
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew test --tests devkor.ontime_back.global.oauth.apple.AppleLoginServiceTest
```

Expected failure:
- `handleRegisterCreatesGuestAppleUserAndDefaultAlarmSettings` fails because the test expects `createAccessToken("new@example.com", 2L)`.

Revert the mutation and rerun the same command. It must pass.

#### 3.3 Kakao Register Uses Unsaved User ID
Temporary mutation:
- In `KakaoLoginFilter.handleRegister`, replace:

```java
jwtTokenProvider.createAccessToken(savedUser.getEmail(), savedUser.getId())
```

with:

```java
jwtTokenProvider.createAccessToken(newUser.getEmail(), newUser.getId())
```

Run:

```bash
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew test --tests devkor.ontime_back.global.oauth.OAuthLoginFilterValidationTest
```

Expected failure:
- `kakaoLoginFilterRegistersNewGuestUser` fails because the test expects `createAccessToken("kakao@example.com", 2L)`.

Revert the mutation and rerun the same command. It must pass.

### 4. Alarm Status Validation Mutations

#### 4.1 Inverted Schedule Window Is Accepted
Temporary mutation:
- In `AlarmService.validateAlarmStatusReport`, remove or invert the branch that rejects:

```java
requestDto.getScheduleWindowEnd().isBefore(requestDto.getScheduleWindowStart())
```

Run:

```bash
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew test --tests devkor.ontime_back.service.AlarmServiceTest
```

Expected failure:
- `reportAlarmStatusRejectsInvalidScheduleWindow` fails because invalid schedule windows no longer throw `GeneralException(INVALID_INPUT)`.

Revert and rerun. The focused suite must pass.

#### 4.2 Unknown Permission Issue Is Accepted
Temporary mutation:
- In `AlarmService.validateAlarmStatusReport`, remove or loosen:

```java
requestDto.getPermissionIssue() != null && !PERMISSION_ISSUES.contains(requestDto.getPermissionIssue())
```

Run the same `AlarmServiceTest` command.

Expected failure:
- `reportAlarmStatusRejectsInvalidPermissionIssue` fails.

Revert and rerun.

#### 4.3 Unknown Failure Reason Is Accepted
Temporary mutation:
- In `AlarmService.validateAlarmStatusReport`, remove or loosen the failure reason check:

```java
failure == null || failure.getReason() == null || !FAILURE_REASONS.contains(failure.getReason())
```

Run the same `AlarmServiceTest` command.

Expected failure:
- `reportAlarmStatusRejectsInvalidFailureReason` fails.

Revert and rerun.

#### 4.4 Unsupported Status With Local Notification Fallback Is Accepted
Temporary mutation:
- In `AlarmService.validateAlarmStatusReport`, remove or loosen:

```java
"unsupported".equals(requestDto.getStatus())
        && (!NATIVE_NONE.equals(requestDto.getNativeAlarmProvider())
        || FALLBACK_LOCAL_NOTIFICATION.equals(requestDto.getFallbackProvider()))
```

Run the same `AlarmServiceTest` command.

Expected failure:
- `reportAlarmStatusRejectsUnsupportedWithFallbackCoverage` fails.

Revert and rerun.

### 5. Stale Native Alarm Suppression Mutation
Temporary mutation:
- In `AlarmService.shouldSuppressLegacyReminder`, remove or loosen:

```java
status.getReconciledAt() == null || status.getReconciledAt().isBefore(Instant.now().minus(Duration.ofHours(24)))
```

Run:

```bash
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew test --tests devkor.ontime_back.service.AlarmServiceTest
```

Expected failure:
- `shouldSuppressLegacyReminderReturnsFalseForStaleAlarmStatus` fails because stale native status incorrectly suppresses the legacy reminder.

Revert and rerun. The focused suite must pass.

### 6. Alarm Settings DTO Validation Mutations

#### 6.1 Unknown Fields Are Accepted
Temporary mutation:
- In `AlarmSettingsPatchDto.isKnownFieldsOnly`, return `true` unconditionally.

Run:

```bash
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew test --tests devkor.ontime_back.dto.AlarmSettingsPatchDtoTest
```

Expected failure:
- `rejectsUnknownAlarmSettingFields` fails.

Revert and rerun.

#### 6.2 Non-Integral Offset Is Accepted
Temporary mutation:
- In `AlarmSettingsPatchDto.getDefaultAlarmOffsetMinutesValue`, accept `Double` by truncating or rounding.

Run the same DTO test command.

Expected failure:
- `rejectsNonIntegralDefaultAlarmOffset` fails.

Revert and rerun.

### 7. Final Verification
After every mutation has been reverted:

1. Confirm no mutation leftovers:

```bash
git diff -- ontime-back/src/main/java/devkor/ontime_back/global/oauth/google/GoogleLoginService.java \
             ontime-back/src/main/java/devkor/ontime_back/global/oauth/apple/AppleLoginService.java \
             ontime-back/src/main/java/devkor/ontime_back/global/oauth/kakao/KakaoLoginFilter.java \
             ontime-back/src/main/java/devkor/ontime_back/service/AlarmService.java \
             ontime-back/src/main/java/devkor/ontime_back/dto/AlarmSettingsPatchDto.java
```

Only intentional non-mutation work should remain.

2. Run full verification:

```bash
cd ontime-back
JAVA_HOME=/Users/ejunpark/.sdkman/candidates/java/current \
PATH=/Users/ejunpark/.sdkman/candidates/java/current/bin:$PATH \
./gradlew clean check
```

3. Confirm coverage:

```bash
python3 - <<'PY'
import xml.etree.ElementTree as ET
root = ET.parse('build/reports/jacoco/test/jacocoTestReport.xml').getroot()
counter = next(c for c in root.findall('counter') if c.attrib['type'] == 'INSTRUCTION')
missed = int(counter.attrib['missed'])
covered = int(counter.attrib['covered'])
total = missed + covered
print(f'instruction coverage: {covered}/{total} = {covered / total:.2%}')
PY
```

Expected final result:
- `BUILD SUCCESSFUL`
- Instruction coverage remains `>= 80%`.
- No temporary mutation remains in production code.

## Validation
- Each targeted mutation causes at least one targeted test failure.
- Each targeted test passes again after reverting the mutation.
- Full `./gradlew clean check` passes after all mutations are reverted.
- The final JaCoCo report remains above the `0.80` gate.

## Done Criteria
- A short mutation log exists in the implementer's notes or PR description with:
  - Mutation name.
  - Test command.
  - Expected failing test.
  - Confirmation that the test passed again after revert.
- No mutation code remains.
- The test suite still passes with the 80% coverage gate.

## Open Questions
- None.
