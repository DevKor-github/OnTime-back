package devkor.ontime_back.response;

import devkor.ontime_back.logging.RequestLogPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void scheduleLifecycleErrorsUseSymbolicCodeForFrontendBranching() {
        ResponseEntity<ApiResponseForm<Void>> response = handler.handleGeneralException(
                new GeneralException(ErrorCode.SCHEDULE_ALREADY_STARTED)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo("error");
        assertThat(response.getBody().getCode()).isEqualTo("SCHEDULE_ALREADY_STARTED");
    }

    @Test
    void ordinaryBusinessErrorsUseNumericApplicationCode() {
        ResponseEntity<ApiResponseForm<Void>> response = handler.handleGeneralException(
                new GeneralException(ErrorCode.USER_NOT_FOUND)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void invalidTokenExceptionReturnsUnauthorizedEnvelope() {
        ResponseEntity<ApiResponseForm<Void>> response = handler.handleInvalidTokenException(
                new InvalidTokenException("bad token"),
                new MockHttpServletRequest()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo("error");
        assertThat(response.getBody().getCode()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("bad token");
    }

    @Test
    void unreadableJsonResponseIncludesRequestIdHeaderAndValidationBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/schedules");
        request.addHeader(RequestLogPolicy.REQUEST_ID_HEADER, "request-99");

        ResponseEntity<ApiResponseForm<ValidationErrorResponse>> response =
                handler.handleHttpMessageNotReadableException(
                        new HttpMessageNotReadableException("bad json", emptyInputMessage()),
                        request
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getFirst(RequestLogPolicy.REQUEST_ID_HEADER)).isEqualTo("request-99");
        assertThat(response.getBody().getData().errors())
                .containsExactly(new ValidationErrorResponse.FieldError("request", "요청 형식이 올바르지 않습니다."));
    }

    @Test
    void typeMismatchResponseNamesTheBadParameter() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "not-a-number",
                Long.class,
                "scheduleId",
                (MethodParameter) null,
                new NumberFormatException("bad")
        );

        ResponseEntity<ApiResponseForm<ValidationErrorResponse>> response =
                handler.handleMethodArgumentTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData().errors())
                .containsExactly(new ValidationErrorResponse.FieldError("scheduleId", "요청 값의 형식이 올바르지 않습니다."));
    }

    @Test
    void missingParameterResponseNamesTheRequiredParameter() {
        ResponseEntity<ApiResponseForm<ValidationErrorResponse>> response =
                handler.handleMissingServletRequestParameterException(
                        new MissingServletRequestParameterException("from", "LocalDate")
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData().errors())
                .containsExactly(new ValidationErrorResponse.FieldError("from", "필수 요청 값입니다."));
    }

    @Test
    void handlerMethodValidationNamesIndexedAndKeyedParameters() throws Exception {
        Method method = SampleController.class.getDeclaredMethod("sample", List.class, java.util.Map.class);
        ParameterValidationResult indexedResult = new ParameterValidationResult(
                new MethodParameter(method, 0),
                List.of("bad"),
                List.of(new DefaultMessageSourceResolvable(new String[]{"schedules[0]"}, "invalid item")),
                List.of("bad"),
                0,
                null
        );
        ParameterValidationResult keyedResult = new ParameterValidationResult(
                new MethodParameter(method, 1),
                java.util.Map.of("from", "bad"),
                List.of(new DefaultMessageSourceResolvable(new String[]{"range[from]"}, "invalid range")),
                java.util.Map.of("from", "bad"),
                null,
                "from"
        );
        HandlerMethodValidationException exception = new HandlerMethodValidationException(
                MethodValidationResult.create(new SampleController(), method, List.of(indexedResult, keyedResult))
        );

        ResponseEntity<ApiResponseForm<ValidationErrorResponse>> response =
                handler.handleHandlerMethodValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData().errors())
                .containsExactly(
                        new ValidationErrorResponse.FieldError("request[0]", "invalid item"),
                        new ValidationErrorResponse.FieldError("request[from]", "invalid range")
                );
    }

    private HttpInputMessage emptyInputMessage() {
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                return new org.springframework.http.HttpHeaders();
            }
        };
    }

    @SuppressWarnings("unused")
    private static class SampleController {
        void sample(List<String> schedules, java.util.Map<String, String> range) {
        }
    }
}
