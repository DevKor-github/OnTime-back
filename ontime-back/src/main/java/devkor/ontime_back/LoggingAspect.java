package devkor.ontime_back;

import devkor.ontime_back.dto.RequestInfoDto;
import devkor.ontime_back.entity.ApiLog;
import devkor.ontime_back.repository.ApiLogRepository;
import devkor.ontime_back.response.GeneralException;
import devkor.ontime_back.service.ApiLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.util.Map;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private final ApiLogService apiLogService;
    private static final String NO_PARAMS = "No Params";
    private static final String NO_BODY = "No Body";

    @Pointcut("bean(*Controller)")
    private void allRequest() {}

    @Around("allRequest()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestInfoDto requestInfoDto = extractRequestInfo();

        // requestTime
        long beforeRequest = System.currentTimeMillis();

        // pathVariable, requestBody
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();

        String pathVariable = null;
        String requestBody = null;

        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (annotation instanceof PathVariable) {
                    pathVariable = args[i].toString(); // @PathVariable 값 저장
                } else if (annotation instanceof RequestBody) {
                    requestBody = args[i].toString(); // @RequestBody 값 저장
                }
            }
        }

        // responseStatus
        int responseStatus = 200;
        Object result;
        try {
            // 실제 메서드 실행
            result = joinPoint.proceed();
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                responseStatus = responseEntity.getStatusCodeValue(); // 상태 코드 추출
            }

            // 정상 요청 로그 저장
            long timeTaken = System.currentTimeMillis() - beforeRequest;

            ApiLog apiLog = buildApiLog(requestInfoDto, responseStatus, timeTaken);
            apiLogService.saveLog(apiLog);

            log.info("[Request Log] requestUrl: {}, requestMethod: {}, userId: {}, clientIp: {}, pathVariable: {}, requestBody: {}, responseStatus: {}, timeTaken: {}",
                    requestInfoDto.getRequestUrl(), requestInfoDto.getRequestMethod(), requestInfoDto.getUserId(), requestInfoDto.getClientIp(),
                    pathVariable != null ? pathVariable : NO_PARAMS,
                    requestBody != null ? requestBody : NO_BODY,
                    responseStatus, timeTaken);

            return result;

        } catch (Exception ex) {
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "allRequest()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Exception ex) {
        RequestInfoDto requestInfoDto = extractRequestInfo();

        // exceptionName
        String exceptionName;
        if (ex instanceof GeneralException) {
            exceptionName = ((GeneralException) ex).getErrorCode().name();
        } else {
            exceptionName = ex.getClass().getSimpleName();
        };
        // exceptionMessage
        String exceptionMessage = ex.getMessage();
        // responseStatus
        int responseStatus = mapExceptionToStatusCode(ex);

        log.error("[Error Log] requestUrl: {}, requestMethod: {}, userId: {}, clientIp: {}, exception: {}, message: {}, responseStatus: {}",
                requestInfoDto.getRequestUrl(), requestInfoDto.getRequestMethod(), requestInfoDto.getUserId(), requestInfoDto.getClientIp(), exceptionName, exceptionMessage, responseStatus);

        ApiLog errorLog = buildApiLog(requestInfoDto, responseStatus, 0);
        apiLogService.saveLog(errorLog);
    }

    // requestinfo 추출
    private RequestInfoDto extractRequestInfo() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : "Anonymous";
        String clientIp = request.getRemoteAddr();

        return new RequestInfoDto(requestUrl, requestMethod, userId, clientIp);
    }

    // apilog 생성
    private ApiLog buildApiLog(RequestInfoDto info, int responseStatus, long timeTaken) {
        return ApiLog.builder()
                .requestUrl(info.getRequestUrl())
                .requestMethod(info.getRequestMethod())
                .userId(info.getUserId())
                .clientIp(info.getClientIp())
                .responseStatus(responseStatus)
                .takenTime(timeTaken)
                .build();
    }

    // Exception 매핑
    private static final Map<Class<? extends Exception>, Integer> EXCEPTION_STATUS_MAP = Map.of(
            IllegalArgumentException.class, 400,
            AccessDeniedException.class, 403,
            MethodArgumentNotValidException.class, 422
    );

    private int mapExceptionToStatusCode(Exception e) {
        return EXCEPTION_STATUS_MAP.getOrDefault(e.getClass(), 500);
    }

}
