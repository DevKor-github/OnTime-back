package devkor.ontime_back.response;

import devkor.ontime_back.logging.RequestLogPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.List;


@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponseForm<Void>> handleGeneralException(GeneralException e) {
        // GeneralException에서 ErrorCode를 가져와 처리
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponseForm.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponseForm<Void>> handleInvalidTokenException(InvalidTokenException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseForm.error(401, ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseForm<ValidationErrorResponse>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = RequestLogPolicy.resolveRequestId(request);

        log.error("[Error Log] requestId: {}, route: {}, method: {}, actor: {}, clientIp: {}, exception: {}, responseStatus: {}",
                requestId, request.getRequestURI(), request.getMethod(), (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : "Anonymous", request.getRemoteAddr(), "HttpMessageNotReadableException", 400);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(RequestLogPolicy.REQUEST_ID_HEADER, requestId)
                .body(validationError(List.of(new ValidationErrorResponse.FieldError("request", "요청 형식이 올바르지 않습니다."))));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseForm<ValidationErrorResponse>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<ValidationErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .sorted(Comparator.comparing(ValidationErrorResponse.FieldError::field))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseForm<ValidationErrorResponse>> handleConstraintViolationException(ConstraintViolationException ex) {
        List<ValidationErrorResponse.FieldError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationErrorResponse.FieldError(fieldName(violation.getPropertyPath().toString()), violation.getMessage()))
                .sorted(Comparator.comparing(ValidationErrorResponse.FieldError::field))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError(errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponseForm<ValidationErrorResponse>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        List<ValidationErrorResponse.FieldError> errors = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationErrorResponse.FieldError(parameterName(result), error.getDefaultMessage())))
                .sorted(Comparator.comparing(ValidationErrorResponse.FieldError::field))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError(errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseForm<ValidationErrorResponse>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(validationError(List.of(new ValidationErrorResponse.FieldError(ex.getName(), "요청 값의 형식이 올바르지 않습니다."))));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseForm<ValidationErrorResponse>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(validationError(List.of(new ValidationErrorResponse.FieldError(ex.getParameterName(), "필수 요청 값입니다."))));
    }

    private ValidationErrorResponse.FieldError toValidationError(FieldError fieldError) {
        return new ValidationErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ApiResponseForm<ValidationErrorResponse> validationError(List<ValidationErrorResponse.FieldError> errors) {
        return ValidationErrorWriter.validationError(errors);
    }

    private String fieldName(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    private String parameterName(org.springframework.validation.method.ParameterValidationResult result) {
        String name = result.getMethodParameter().getParameterName();
        if (name == null || name.isBlank()) {
            name = "request";
        }
        if (result.getContainerIndex() != null) {
            return name + "[" + result.getContainerIndex() + "]";
        }
        if (result.getContainerKey() != null) {
            return name + "[" + result.getContainerKey() + "]";
        }
        return name;
    }

}
