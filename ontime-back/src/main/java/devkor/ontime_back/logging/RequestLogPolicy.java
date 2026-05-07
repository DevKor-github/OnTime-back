package devkor.ontime_back.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestLogPolicy {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = RequestLogPolicy.class.getName() + ".requestId";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "authorization",
            "authCode",
            "clientSecret",
            "client_secret",
            "currentPassword",
            "firebaseToken",
            "idToken",
            "newPassword",
            "password",
            "refreshToken",
            "secret",
            "token"
    );

    private static final Set<String> SAFE_FIELD_ALLOWLIST = Set.of(
            "appVersion",
            "armedScheduleCount",
            "clientIp",
            "deviceId",
            "method",
            "osVersion",
            "platform",
            "requestId",
            "responseStatus",
            "route",
            "timeTakenMs",
            "userId"
    );

    private RequestLogPolicy() {
    }

    public static String resolveRequestId(HttpServletRequest request) {
        Object existingRequestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existingRequestId instanceof String existing && !existing.isBlank()) {
            return existing;
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || !SAFE_REQUEST_ID.matcher(requestId).matches()) {
            requestId = UUID.randomUUID().toString();
        }

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }

    public static void exposeRequestId(ServletRequestAttributes attributes, String requestId) {
        HttpServletResponse response = attributes.getResponse();
        if (response != null) {
            response.setHeader(REQUEST_ID_HEADER, requestId);
        }
    }

    public static boolean isSafeFieldForLogging(String fieldName) {
        return SAFE_FIELD_ALLOWLIST.contains(fieldName) && !isSensitiveFieldName(fieldName);
    }

    public static boolean isSensitiveFieldName(String fieldName) {
        return SENSITIVE_FIELD_NAMES.stream()
                .anyMatch(sensitiveField -> sensitiveField.equalsIgnoreCase(fieldName));
    }
}
