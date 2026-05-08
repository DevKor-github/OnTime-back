package devkor.ontime_back.response;

import java.util.List;

public record ValidationErrorResponse(List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }
}
