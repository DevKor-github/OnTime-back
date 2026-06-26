package devkor.ontime_back;

import devkor.ontime_back.entity.ApiLog;
import devkor.ontime_back.logging.RequestLogPolicy;
import devkor.ontime_back.response.ErrorCode;
import devkor.ontime_back.response.GeneralException;
import devkor.ontime_back.service.ApiLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoggingAspectTest {

    private ApiLogService apiLogService;
    private LoggingAspect loggingAspect;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        apiLogService = mock(ApiLogService.class);
        loggingAspect = new LoggingAspect(apiLogService);
        response = new MockHttpServletResponse();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user-7", null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void logRequestStoresSuccessfulResponseStatusAndExposesRequestId() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/alarm/status");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader(RequestLogPolicy.REQUEST_ID_HEADER, "request-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body("ok"));

        Object result = loggingAspect.logRequest(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        assertThat(response.getHeader(RequestLogPolicy.REQUEST_ID_HEADER)).isEqualTo("request-1");
        ArgumentCaptor<ApiLog> captor = ArgumentCaptor.forClass(ApiLog.class);
        verify(apiLogService).saveLog(captor.capture());
        ApiLog log = captor.getValue();
        assertThat(log.getRequestUrl()).isEqualTo("/alarm/status");
        assertThat(log.getRequestMethod()).isEqualTo("POST");
        assertThat(log.getUserId()).isEqualTo("user-7");
        assertThat(log.getClientIp()).isEqualTo("203.0.113.10");
        assertThat(log.getResponseStatus()).isEqualTo(201);
        assertThat(log.getTakenTime()).isNotNegative();
    }

    @Test
    void logRequestStoresBusinessErrorStatusBeforeRethrowing() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/alarm/settings");
        request.setRemoteAddr("203.0.113.20");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(new GeneralException(ErrorCode.DEVICE_SESSION_NOT_ACTIVE));

        assertThatThrownBy(() -> loggingAspect.logRequest(joinPoint))
                .isInstanceOf(GeneralException.class);

        ArgumentCaptor<ApiLog> captor = ArgumentCaptor.forClass(ApiLog.class);
        verify(apiLogService).saveLog(captor.capture());
        assertThat(captor.getValue().getResponseStatus()).isEqualTo(ErrorCode.DEVICE_SESSION_NOT_ACTIVE.getCode());
    }

    @Test
    void logRequestStoresServerErrorStatusForUnexpectedExceptions() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> loggingAspect.logRequest(joinPoint))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<ApiLog> captor = ArgumentCaptor.forClass(ApiLog.class);
        verify(apiLogService).saveLog(captor.capture());
        assertThat(captor.getValue().getResponseStatus()).isEqualTo(500);
    }
}
