package devkor.ontime_back.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ValidationErrorWriter {

    private ValidationErrorWriter() {
    }

    public static <T> void write(HttpServletResponse response,
                                 ObjectMapper objectMapper,
                                 Set<ConstraintViolation<T>> violations) throws IOException {
        List<ValidationErrorResponse.FieldError> errors = violations.stream()
                .map(violation -> new ValidationErrorResponse.FieldError(
                        fieldName(violation),
                        violation.getMessage()))
                .sorted(Comparator.comparing(ValidationErrorResponse.FieldError::field))
                .toList();

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), validationError(errors));
    }

    public static ApiResponseForm<ValidationErrorResponse> validationError(List<ValidationErrorResponse.FieldError> errors) {
        return ApiResponseForm.error(
                ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.INVALID_INPUT.getMessage(),
                new ValidationErrorResponse(errors));
    }

    private static String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
