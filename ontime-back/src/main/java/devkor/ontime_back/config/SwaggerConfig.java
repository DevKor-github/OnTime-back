package devkor.ontime_back.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://3.38.172.54.nip.io", description = "New Production Server"),
                @Server(url = "http://localhost:8080", description = "Local Server")
        }
)
public class SwaggerConfig {
    private static final String ACCESS_TOKEN_SCHEME = "accessToken";
    private static final String REFRESH_TOKEN_SCHEME = "refreshToken";
    private static final String JSON = "application/json";
    private static final String TEXT = "text/plain";
    private static final String HTML = "text/html";
    private static final ObjectMapper EXAMPLE_OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/",
            "/health",
            "/account-deletion",
            "/privacy-policy",
            "/sign-up",
            "/login",
            "/oauth2/google/login",
            "/oauth2/kakao/login",
            "/oauth2/apple/login",
            "/swagger-ui.html",
            "/error"
    );

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/css",
            "/images",
            "/js",
            "/favicon.ico",
            "/h2-console"
    );

    private static final Set<String> NO_REQUEST_BODY_OPERATIONS = Set.of(
            "POST /friends/links",
            "POST /firebase-token/push-test",
            "POST /schedules/{scheduleId}/start",
            "PUT /users/me/punctuality-score",
            "PUT /users/me/settings/reset"
    );

    private static final Set<String> CONFLICT_OPERATIONS = Set.of(
            "PATCH /users/me/alarm-settings",
            "PUT /users/me/devices/current",
            "DELETE /users/me/devices/current",
            "POST /users/me/alarm-status",
            "POST /schedules/{scheduleId}/start",
            "PUT /schedules/{scheduleId}",
            "DELETE /schedules/{scheduleId}",
            "PUT /schedules/{scheduleId}/finish",
            "POST /schedules/{scheduleId}/preparations",
            "PUT /schedules/{scheduleId}/preparations"
    );

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(ACCESS_TOKEN_SCHEME, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Send as `Authorization: Bearer <access token>`.")
                        )
                        .addSecuritySchemes(REFRESH_TOKEN_SCHEME, new SecurityScheme()
                                .name("Authorization-refresh")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Send as `Authorization-refresh: Bearer <refresh token>` to reissue an access token.")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(ACCESS_TOKEN_SCHEME))
                .info(apiInfo());
    }

    @Bean
    public OpenApiCustomizer coherentOperationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (pathItem == null) {
                    return;
                }

                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    String operationKey = operationKey(httpMethod, path);
                    if (httpMethod == PathItem.HttpMethod.GET) {
                        operation.setRequestBody(null);
                    }
                    if (isPublicPath(path)) {
                        operation.setSecurity(Collections.emptyList());
                    }
                    removeStaleResponseExamples(operation);
                    addRequestExamples(operationKey, httpMethod, operation);
                    addResponseExamples(operationKey, path, httpMethod, operation);
                });
            });
        };
    }

    private Info apiInfo() {
        return new Info()
                .title("Ontime")
                .description("""
                        Ontime API 명세서

                        [JWT 인증 과정]
                        공개 엔드포인트(`/sign-up`, `/login`, `/oauth2/google/login`, `/oauth2/kakao/login`, `/oauth2/apple/login`, `/health`, `/account-deletion`, `/privacy-policy`)를 제외한 API는 access token이 필요합니다.

                        Access token 요청 형식: `Authorization: Bearer <access token>`

                        Refresh token으로 access token을 재발급할 때는 보호 API 호출 전에 `Authorization-refresh: Bearer <refresh token>` 헤더를 보냅니다. 재발급 성공 시 새 access token은 응답 헤더 `Authorization`으로 반환됩니다.

                        일반 로그인과 소셜 로그인, 회원가입 성공 시 access token은 `Authorization` 헤더로, refresh token은 `Authorization-refresh` 헤더로 반환됩니다.
                        """)
                .version("1.0.0");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path)
                || PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void removeStaleResponseExamples(Operation operation) {
        if (operation.getResponses() == null) {
            return;
        }

        operation.getResponses().values().forEach(apiResponse -> {
            if (apiResponse.getContent() == null) {
                return;
            }

            apiResponse.getContent().values().forEach(mediaType -> {
                mediaType.setExample(null);
                mediaType.setExamples(null);
                if (mediaType.getSchema() != null) {
                    mediaType.getSchema().setExample(null);
                }
            });
        });
    }

    private String operationKey(PathItem.HttpMethod httpMethod, String path) {
        return httpMethod.name() + " " + path;
    }

    private void addRequestExamples(String operationKey, PathItem.HttpMethod httpMethod, Operation operation) {
        if (httpMethod == PathItem.HttpMethod.GET || NO_REQUEST_BODY_OPERATIONS.contains(operationKey)) {
            operation.setRequestBody(null);
            return;
        }

        Map<String, NamedExample> examples = requestExamples(operationKey);
        if (examples.isEmpty()) {
            return;
        }

        RequestBody requestBody = Optional.ofNullable(operation.getRequestBody()).orElseGet(RequestBody::new);
        Content content = Optional.ofNullable(requestBody.getContent()).orElseGet(Content::new);
        MediaType jsonContent = Optional.ofNullable(content.get(JSON)).orElseGet(MediaType::new);
        examples.forEach((name, example) -> jsonContent.addExamples(name, example.toOpenApiExample()));
        content.addMediaType(JSON, jsonContent);
        requestBody.setContent(content);
        operation.setRequestBody(requestBody);
    }

    private void addResponseExamples(String operationKey, String path, PathItem.HttpMethod httpMethod, Operation operation) {
        ApiResponses responses = Optional.ofNullable(operation.getResponses()).orElseGet(ApiResponses::new);

        if (isHtmlEndpoint(path)) {
            ensureResponseExample(responses, "200", HTML, new NamedExample("HTML page", "HTML document returned by the public policy page.", "<!doctype html><html lang=\"ko\"><body>...</body></html>"));
            operation.setResponses(responses);
            return;
        }

        if ("/health".equals(path)) {
            ensureResponseExample(responses, "200", TEXT, new NamedExample("Health check success", "Plain text load-balancer health response.", "Success Health Check"));
            operation.setResponses(responses);
            return;
        }

        ensureResponseExample(responses, "200", JSON, successExample(operationKey, operation));
        ensureResponseExample(responses, "400", JSON, validationErrorExample());
        ensureResponseExample(responses, "400", JSON, malformedJsonExample());

        if (!isPublicPath(path)) {
            ensureResponseExample(responses, "401", JSON, accessTokenEmptyExample());
            ensureResponseExample(responses, "401", JSON, accessTokenInvalidExample());
            ensureResponseExample(responses, "401", JSON, refreshTokenInvalidExample());
        }

        if (path.contains("{scheduleId}") || path.contains("{uuid}")) {
            ensureResponseExample(responses, "404", JSON, notFoundExample(path.contains("{uuid}") ? "친구 요청 링크를 찾을 수 없습니다." : "해당 약속이 존재하지 않습니다."));
        }

        if (CONFLICT_OPERATIONS.contains(operationKey)) {
            ensureResponseExample(responses, "409", JSON, conflictExample(operationKey));
        }

        ensureResponseExample(responses, "500", JSON, unexpectedErrorExample());
        operation.setResponses(responses);
    }

    private boolean isHtmlEndpoint(String path) {
        return path.startsWith("/account-deletion") || path.startsWith("/privacy-policy");
    }

    private void ensureResponseExample(ApiResponses responses, String code, String mediaTypeName, NamedExample namedExample) {
        ApiResponse response = Optional.ofNullable(responses.get(code)).orElseGet(ApiResponse::new);
        Content content = Optional.ofNullable(response.getContent()).orElseGet(Content::new);
        MediaType mediaType = Optional.ofNullable(content.get(mediaTypeName)).orElseGet(MediaType::new);
        mediaType.addExamples(namedExample.name(), namedExample.toOpenApiExample());
        content.addMediaType(mediaTypeName, mediaType);
        response.setContent(content);
        if (response.getDescription() == null || response.getDescription().isBlank()) {
            response.setDescription(defaultDescription(code));
        }
        responses.addApiResponse(code, response);
    }

    private String defaultDescription(String code) {
        return switch (code) {
            case "200" -> "Success";
            case "400" -> "Bad request";
            case "401" -> "Unauthorized";
            case "404" -> "Not found";
            case "409" -> "Conflict";
            case "500" -> "Unexpected server error";
            default -> "Response";
        };
    }

    private Map<String, NamedExample> requestExamples(String operationKey) {
        Map<String, NamedExample> examples = new LinkedHashMap<>();
        switch (operationKey) {
            case "PATCH /users/me/alarm-settings" -> {
                examples.put("valid_partial_update", json("Valid partial update", "Only provided fields are updated.", "{\"alarmsEnabled\":true,\"defaultAlarmOffsetMinutes\":10}"));
                examples.put("invalid_unknown_field", json("Invalid unknown field", "Unknown fields are rejected.", "{\"alarmsEnabled\":\"true\",\"unknown\":1}"));
            }
            case "PUT /users/me/analytics-preference" -> {
                examples.put("enabled", json("Enable analytics", "Allows optional Product Usage Events for the signed-in account.", "{\"enabled\":true}"));
                examples.put("disabled", json("Disable analytics", "Stops optional Product Usage Events for the signed-in account.", "{\"enabled\":false}"));
                examples.put("invalid_unknown_field", json("Invalid unknown field", "Only enabled is accepted.", "{\"enabled\":\"false\",\"unknown\":1}"));
            }
            case "PUT /users/me/devices/current" -> {
                examples.put("valid_ios_device", json("Valid iOS device", "Registers the current access-token session to the device.", "{\"deviceId\":\"ios-device-000001\",\"platform\":\"ios\",\"appVersion\":\"1.2.3\",\"osVersion\":\"iOS 18.0\",\"supportsNativeAlarm\":true,\"nativeAlarmProvider\":\"iosAlarmKit\",\"fallbackProvider\":\"localNotification\"}"));
                examples.put("invalid_device_id", json("Invalid device ID", "deviceId must be 16-128 allowed characters.", "{\"deviceId\":\"short\",\"platform\":\"ios\",\"supportsNativeAlarm\":true,\"nativeAlarmProvider\":\"iosAlarmKit\",\"fallbackProvider\":\"localNotification\"}"));
            }
            case "DELETE /users/me/devices/current" -> {
                examples.put("with_device_id", json("Unregister explicit device", "Optional device ID body.", "{\"deviceId\":\"ios-device-000001\"}"));
                examples.put("without_body", json("Unregister current token-bound device", "Body may be omitted.", "{}"));
            }
            case "POST /users/me/alarm-status" -> {
                examples.put("armed_status", json("Armed status report", "Reports a successfully reconciled native alarm window.", "{\"deviceId\":\"ios-device-000001\",\"reconciledAt\":\"2026-05-05T09:00:00+09:00\",\"scheduleWindowStart\":\"2026-05-05T00:00:00\",\"scheduleWindowEnd\":\"2026-05-06T00:00:00\",\"alarmCoverageStart\":\"2026-05-05T00:00:00\",\"alarmCoverageEnd\":\"2026-05-06T00:00:00\",\"status\":\"armed\",\"permissionIssue\":null,\"nativeAlarmProvider\":\"iosAlarmKit\",\"fallbackProvider\":\"localNotification\",\"armedScheduleCount\":1,\"armedScheduleIds\":[\"3fa85f64-5717-4562-b3fc-2c963f66afe5\"],\"skippedScheduleCount\":0,\"failures\":[]}"));
                examples.put("invalid_range", json("Invalid date range", "scheduleWindowStart must be before scheduleWindowEnd.", "{\"deviceId\":\"ios-device-000001\",\"reconciledAt\":\"2026-05-05T09:00:00+09:00\",\"scheduleWindowStart\":\"2026-05-06T00:00:00\",\"scheduleWindowEnd\":\"2026-05-05T00:00:00\",\"alarmCoverageStart\":\"2026-05-05T00:00:00\",\"alarmCoverageEnd\":\"2026-05-06T00:00:00\",\"status\":\"bad\",\"nativeAlarmProvider\":\"iosAlarmKit\",\"fallbackProvider\":\"localNotification\"}"));
            }
            case "POST /feedback" -> feedbackExamples(examples, "피드백입니다. 이런게 아쉬워요");
            case "POST /firebase-token" -> {
                examples.put("valid_token", json("Valid FCM token", "Stores the FCM token and optionally links it to a registered device.", "{\"firebaseToken\":\"fcm-token-abc123\",\"deviceId\":\"ios-device-000001\"}"));
                examples.put("invalid_token", json("Invalid FCM token", "firebaseToken is required and deviceId must match the device ID format.", "{\"firebaseToken\":\"\",\"deviceId\":\"short\"}"));
            }
            case "POST /friends/{uuid}/approve" -> {
                examples.put("accept", json("Accept friend request", "Accepts the pending friend request.", "{\"acceptStatus\":\"ACCEPTED\"}"));
                examples.put("reject", json("Reject friend request", "Rejects the pending friend request.", "{\"acceptStatus\":\"REJECTED\"}"));
                examples.put("invalid_status", json("Invalid accept status", "Only ACCEPTED or REJECTED is allowed.", "{\"acceptStatus\":\"PENDING\"}"));
            }
            case "POST /schedules/{scheduleId}/preparations", "PUT /schedules/{scheduleId}/preparations", "PUT /users/preparations" -> preparationListExamples(examples);
            case "PUT /schedules/{scheduleId}" -> scheduleModExamples(examples);
            case "POST /schedules" -> scheduleAddExamples(examples);
            case "PUT /schedules/{scheduleId}/finish" -> {
                examples.put("on_time_finish", json("Finish on time", "latenessTime is zero when the user is not late.", "{\"latenessTime\":0}"));
                examples.put("late_finish", json("Finish late", "latenessTime is minutes late and must be 0-1440.", "{\"latenessTime\":12}"));
                examples.put("invalid_lateness", json("Invalid lateness", "Negative latenessTime is rejected.", "{\"latenessTime\":-1}"));
            }
            case "POST /oauth2/google/login" -> {
                examples.put("valid_google_identity", json("Valid Google login", "Google identity token from the client.", "{\"idToken\":\"eyJhbGciOi...\",\"refreshToken\":\"google-refresh-token\"}"));
                examples.put("missing_id_token", json("Missing Google token", "idToken is required.", "{\"refreshToken\":\"google-refresh-token\"}"));
            }
            case "POST /oauth2/kakao/login" -> {
                examples.put("valid_kakao_profile", json("Valid Kakao login", "Kakao profile payload from the client.", "{\"id\":\"4803687123\",\"profile\":{\"nickname\":\"김철수\",\"thumbnailImageUrl\":\"https://example.com/thumb.jpg\",\"profile_image_url\":\"https://example.com/profile.jpg\",\"defaultImage\":false,\"defaultNickname\":false}}"));
                examples.put("missing_profile", json("Missing Kakao profile", "profile is required.", "{\"id\":\"4803687123\"}"));
            }
            case "POST /oauth2/apple/login" -> {
                examples.put("valid_apple_login", json("Valid Apple login", "Apple identity token, auth code, and profile fields.", "{\"idToken\":\"eyJhbGciOi...\",\"authCode\":\"apple-auth-code\",\"fullName\":\"허진서\",\"email\":\"user@example.com\"}"));
                examples.put("missing_auth_code", json("Missing Apple auth code", "authCode is required.", "{\"idToken\":\"eyJhbGciOi...\",\"fullName\":\"허진서\"}"));
            }
            case "DELETE /oauth2/apple/me", "DELETE /oauth2/google/me", "DELETE /users/me/delete" -> {
                examples.put("without_feedback", json("Delete without feedback", "The request body may be omitted.", "{}"));
                feedbackExamples(examples, "탈퇴 피드백입니다.");
            }
            case "POST /sign-up" -> {
                examples.put("valid_signup", json("Valid signup", "Password must include letters, digits, and special characters.", "{\"email\":\"user@example.com\",\"password\":\"password123!\",\"name\":\"junbeom\"}"));
                examples.put("invalid_signup", json("Invalid signup", "Invalid email, weak password, and blank name are rejected.", "{\"email\":\"bad-email\",\"password\":\"password123\",\"name\":\"\"}"));
            }
            case "POST /login" -> {
                examples.put("valid_login", json("Valid login", "General login credentials.", "{\"email\":\"user@example.com\",\"password\":\"password123!\"}"));
                examples.put("invalid_login", json("Invalid login", "Invalid email format and short password are rejected.", "{\"email\":\"bad-email\",\"password\":\"short\"}"));
            }
            case "PUT /users/me/password" -> {
                examples.put("valid_change", json("Valid password change", "New password must include letters, digits, and special characters.", "{\"currentPassword\":\"password123!\",\"newPassword\":\"newPassword123!\"}"));
                examples.put("same_password", json("Same password", "Business error when the new password matches the current password.", "{\"currentPassword\":\"password123!\",\"newPassword\":\"password123!\"}"));
            }
            case "PUT /users/me/spare-time" -> {
                examples.put("valid_spare_time", json("Valid spare time", "newSpareTime must be 0-1440.", "{\"newSpareTime\":30}"));
                examples.put("invalid_spare_time", json("Invalid spare time", "Negative spare time is rejected.", "{\"newSpareTime\":-1}"));
            }
            case "PUT /users/me/onboarding" -> onboardingExamples(examples);
            case "PUT /users/me/settings" -> {
                examples.put("valid_settings", json("Valid settings update", "All fields are optional, but provided numeric values are validated.", "{\"isNotificationsEnabled\":true,\"soundVolume\":75,\"isPlayOnSpeaker\":false,\"is24HourFormat\":true}"));
                examples.put("invalid_volume", json("Invalid volume", "soundVolume must be 0-100.", "{\"soundVolume\":101}"));
            }
            default -> {
            }
        }
        return examples;
    }

    private void feedbackExamples(Map<String, NamedExample> examples, String message) {
        examples.put("with_feedback", json("With feedback", "Optional feedback payload.", "{\"feedbackId\":\"d784cde3-9ff9-4054-872a-500bbcc2198a\",\"message\":\"" + message + "\"}"));
        examples.put("invalid_feedback", json("Invalid feedback", "message must be 1000 characters or fewer.", "{\"message\":\"" + "x".repeat(80) + "...\"}"));
    }

    private void preparationListExamples(Map<String, NamedExample> examples) {
        examples.put("valid_preparations", json("Valid preparation list", "Each item requires an id, name, and time.", "[{\"preparationId\":\"123e4567-e89b-12d3-a456-426614174011\",\"preparationName\":\"기상하기\",\"preparationTime\":10,\"nextPreparationId\":\"123e4567-e89b-12d3-a456-426614174012\"},{\"preparationId\":\"123e4567-e89b-12d3-a456-426614174012\",\"preparationName\":\"세수하기\",\"preparationTime\":10,\"nextPreparationId\":null}]"));
        examples.put("empty_preparations", json("Empty preparation list", "At least one preparation is required.", "[]"));
    }

    private void scheduleAddExamples(Map<String, NamedExample> examples) {
        examples.put("valid_schedule", json("Valid schedule create", "Creates a new schedule and place association.", "{\"scheduleId\":\"3fa85f64-5717-4562-b3fc-2c963f66afe5\",\"placeId\":\"70d460da-6a82-4c57-a285-567cdeda5670\",\"placeName\":\"Home\",\"scheduleName\":\"Birthday Party\",\"moveTime\":10,\"scheduleTime\":\"2026-05-15T19:30:00\",\"isChange\":false,\"isStarted\":false,\"scheduleSpareTime\":20,\"scheduleNote\":\"Write a message.\"}"));
        examples.put("invalid_schedule", json("Invalid schedule create", "Blank names and negative times are rejected.", "{\"scheduleId\":\"3fa85f64-5717-4562-b3fc-2c963f66afe5\",\"placeId\":\"70d460da-6a82-4c57-a285-567cdeda5670\",\"placeName\":\"\",\"scheduleName\":\"\",\"moveTime\":-1,\"scheduleTime\":\"2020-01-01T09:00:00\",\"scheduleSpareTime\":-1}"));
    }

    private void scheduleModExamples(Map<String, NamedExample> examples) {
        examples.put("valid_schedule_update", json("Valid schedule update", "Updates the existing schedule referenced by the path.", "{\"placeId\":\"70d460da-6a82-4c57-a285-567cdeda5670\",\"placeName\":\"Office\",\"scheduleName\":\"Team Meeting\",\"moveTime\":20,\"scheduleTime\":\"2026-05-15T09:30:00\",\"scheduleSpareTime\":10,\"scheduleNote\":\"Bring laptop\",\"latenessTime\":0}"));
        examples.put("invalid_schedule_update", json("Invalid schedule update", "Required names and non-negative times are validated.", "{\"placeId\":\"70d460da-6a82-4c57-a285-567cdeda5670\",\"placeName\":\"\",\"scheduleName\":\"\",\"moveTime\":-1,\"scheduleTime\":\"2026-05-15T09:30:00\"}"));
    }

    private void onboardingExamples(Map<String, NamedExample> examples) {
        examples.put("valid_onboarding", json("Valid onboarding", "Sets spare time, note, and first preparation list.", "{\"spareTime\":30,\"note\":\"내 인생에 지각은 없다!!!\",\"preparationList\":[{\"preparationId\":\"123e4567-e89b-12d3-a456-426614174011\",\"preparationName\":\"기상하기\",\"preparationTime\":10,\"nextPreparationId\":null}]}"));
        examples.put("invalid_onboarding", json("Invalid onboarding", "preparationList must not be empty and spareTime must be 0-1440.", "{\"spareTime\":-1,\"note\":\"too early\",\"preparationList\":[]}"));
    }

    private NamedExample successExample(String operationKey, Operation operation) {
        String message = Optional.ofNullable(operation.getSummary()).orElse("요청") + " 성공";
        String data = successData(operationKey);
        return json("Success", "Successful response body.", "{\"status\":\"success\",\"code\":200,\"message\":\"" + escape(message) + "\",\"data\":" + data + "}");
    }

    private String successData(String operationKey) {
        return switch (operationKey) {
            case "GET /users/me/alarm-settings" -> "{\"alarmsEnabled\":true,\"defaultAlarmOffsetMinutes\":10,\"updatedAt\":\"2026-05-05T00:00:00Z\"}";
            case "PATCH /users/me/alarm-settings" -> "{\"alarmsEnabled\":false,\"defaultAlarmOffsetMinutes\":5,\"updatedAt\":\"2026-05-05T00:00:00Z\"}";
            case "GET /users/me/analytics-preference" -> "{\"enabled\":false,\"updatedAt\":\"2026-05-26T12:00:00Z\"}";
            case "PUT /users/me/analytics-preference" -> "{\"enabled\":true,\"updatedAt\":\"2026-05-26T12:00:05Z\"}";
            case "PUT /users/me/devices/current" -> "{\"deviceId\":\"ios-device-000001\",\"active\":true,\"lastSeenAt\":\"2026-05-05T00:00:00Z\"}";
            case "DELETE /users/me/devices/current" -> "{\"active\":false}";
            case "POST /users/me/alarm-status" -> "{\"received\":true}";
            case "GET /users/me/alarm-status" -> "{\"deviceId\":\"ios-device-000001\",\"active\":true,\"platform\":\"ios\",\"appVersion\":\"1.2.3\",\"osVersion\":\"iOS 18.0\",\"supportsNativeAlarm\":true,\"nativeAlarmProvider\":\"iosAlarmKit\",\"fallbackProvider\":\"localNotification\",\"lastSeenAt\":\"2026-05-05T00:00:00Z\",\"reconciledAt\":\"2026-05-05T00:00:00Z\",\"scheduleWindowStart\":\"2026-05-05T00:00:00\",\"scheduleWindowEnd\":\"2026-05-06T00:00:00\",\"alarmCoverageStart\":\"2026-05-05T00:00:00\",\"alarmCoverageEnd\":\"2026-05-06T00:00:00\",\"status\":\"armed\",\"permissionIssue\":null,\"armedScheduleCount\":1,\"armedScheduleIds\":[\"3fa85f64-5717-4562-b3fc-2c963f66afe5\"],\"skippedScheduleCount\":0,\"failures\":[],\"updatedAt\":\"2026-05-05T00:00:00Z\"}";
            case "GET /documents/terms", "GET /documents/privacy", "GET /documents/ontime-description" -> "\"문서 본문입니다.\"";
            case "POST /friends/links" -> "{\"friendShipId\":\"3fa85f64-5717-4562-b3fc-2c963f66afe5\"}";
            case "GET /friends/{uuid}/requests" -> "{\"requesterId\":2,\"requesterName\":\"junbeom\",\"requesterEmail\":\"requester@example.com\"}";
            case "GET /friends" -> "{\"friendsList\":[{\"friendId\":2,\"friendName\":\"junbeom\",\"friendEmail\":\"friend@example.com\"}]}";
            case "GET /schedules", "GET /schedules/alarm-window" -> "[" + scheduleData() + "]";
            case "GET /schedules/{scheduleId}" -> scheduleData();
            case "POST /schedules/{scheduleId}/start" -> "{\"scheduleId\":\"3fa85f64-5717-4562-b3fc-2c963f66afe5\",\"startedAt\":\"2026-05-15T08:40:00\",\"preparationCount\":2}";
            case "GET /schedules/lateness-history" -> "[{\"scheduleId\":\"3fa85f64-5717-4562-b3fc-2c963f66afe5\",\"scheduleName\":\"Morning meeting\",\"scheduleTime\":\"2026-05-15T09:30:00\",\"latenessTime\":12}]";
            case "GET /schedules/{scheduleId}/preparations", "GET /users/preparations" -> "[{\"preparationId\":\"123e4567-e89b-12d3-a456-426614174011\",\"preparationName\":\"기상하기\",\"preparationTime\":10,\"nextPreparationId\":null}]";
            case "GET /users/me/punctuality-score" -> "{\"punctualityScore\":97.5}";
            case "GET /users/me" -> "{\"userId\":1,\"email\":\"user@example.com\",\"name\":\"junbeom\",\"spareTime\":30,\"note\":\"내 인생에 지각은 없다!!!\",\"punctualityScore\":97.5,\"role\":\"USER\",\"socialType\":null}";
            case "POST /sign-up" -> "{\"userId\":1,\"email\":\"user@example.com\",\"name\":\"junbeom\",\"spareTime\":null,\"note\":null,\"punctualityScore\":null,\"role\":\"GUEST\",\"socialType\":null}";
            case "POST /login", "POST /oauth2/google/login", "POST /oauth2/apple/login" -> "{\"userId\":1,\"email\":\"user@example.com\",\"name\":\"junbeom\",\"spareTime\":10,\"note\":null,\"punctualityScore\":100.0,\"role\":\"USER\"}";
            case "POST /oauth2/kakao/login" -> "{\"message\":\"회원가입이 완료되었습니다. ROLE이 GUEST이므로 온보딩이 필요합니다.\",\"role\":\"GUEST\"}";
            default -> "null";
        };
    }

    private String scheduleData() {
        return "{\"scheduleId\":\"3fa85f64-5717-4562-b3fc-2c963f66afe5\",\"place\":{\"placeId\":\"70d460da-6a82-4c57-a285-567cdeda5670\",\"placeName\":\"Office\"},\"scheduleName\":\"Morning meeting\",\"moveTime\":20,\"scheduleTime\":\"2026-05-15T09:30:00\",\"scheduleSpareTime\":10,\"scheduleNote\":\"Bring laptop\",\"latenessTime\":null,\"doneStatus\":\"NOT_STARTED\"}";
    }

    private NamedExample validationErrorExample() {
        return json("Validation error", "Bean validation or parameter validation failed.", "{\"status\":\"error\",\"code\":1002,\"message\":\"유효하지 않은 입력값입니다.\",\"data\":{\"errors\":[{\"field\":\"request\",\"message\":\"필수 요청 값입니다.\"}]}}");
    }

    private NamedExample malformedJsonExample() {
        return json("Malformed JSON", "Request body could not be parsed.", "{\"status\":\"error\",\"code\":1002,\"message\":\"유효하지 않은 입력값입니다.\",\"data\":{\"errors\":[{\"field\":\"request\",\"message\":\"요청 형식이 올바르지 않습니다.\"}]}}");
    }

    private NamedExample accessTokenEmptyExample() {
        return json("Missing access token", "Authorization header was omitted.", "{\"status\":\"accessTokenEmpty\",\"code\":401,\"message\":\"Unauthorized: You must authenticate to access this resource.\",\"data\":null}");
    }

    private NamedExample accessTokenInvalidExample() {
        return json("Invalid access token", "Authorization bearer token is expired, unknown, or invalid.", "{\"status\":\"accessTokenInvalid\",\"code\":401,\"message\":\"Unauthorized: You must authenticate to access this resource.\",\"data\":null}");
    }

    private NamedExample refreshTokenInvalidExample() {
        return json("Invalid refresh token", "Authorization-refresh bearer token is expired, unknown, or invalid.", "{\"status\":\"refreshTokenInvalid\",\"code\":401,\"message\":\"Unauthorized: You must authenticate to access this resource.\",\"data\":null}");
    }

    private NamedExample notFoundExample(String message) {
        return json("Not found", "Referenced path resource does not exist or is not visible to the user.", "{\"status\":\"error\",\"code\":1010,\"message\":\"" + escape(message) + "\",\"data\":null}");
    }

    private NamedExample conflictExample(String operationKey) {
        String message = operationKey.contains("devices") || operationKey.contains("alarm-status")
                ? "DEVICE_SESSION_NOT_ACTIVE"
                : "Started or finished schedules cannot be edited.";
        return json("Conflict", "Current resource state does not allow the requested operation.", "{\"status\":\"error\",\"code\":409,\"message\":\"" + escape(message) + "\",\"data\":null}");
    }

    private NamedExample unexpectedErrorExample() {
        return json("Unexpected error", "Unhandled server error response.", "{\"status\":\"error\",\"code\":1000,\"message\":\"Unexpected Error: An unexpected error occurred.\",\"data\":null}");
    }

    private NamedExample json(String name, String description, String value) {
        try {
            return new NamedExample(name, description, EXAMPLE_OBJECT_MAPPER.readTree(value));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid Swagger example JSON: " + name, e);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record NamedExample(String name, String description, Object value) {
        private Example toOpenApiExample() {
            return new Example()
                    .summary(name)
                    .description(description)
                    .value(value);
        }
    }
}
