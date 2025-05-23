package devkor.ontime_back.response;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


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
    public ResponseEntity<ApiResponseForm<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.error("[Error Log] requestUrl: {}, requestMethod: {}, userId: {}, clientIp: {}, exception: {}, message: {}, responseStatus: {}",
                request.getRequestURI(), request.getMethod(), (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : "Anonymous", request.getRemoteAddr(), "HttpMessageNotReadableException", "요청 형식이 올바르지 않습니다.", 400);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseForm.error(400, "요청 형식이 올바르지 않습니다."));
    }

}