package devkor.ontime_back;

import devkor.ontime_back.entity.ApiLog;
import devkor.ontime_back.repository.ApiLogRepository;
import devkor.ontime_back.response.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;


@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private final ApiLogRepository apiLogRepository;

    public LoggingAspect(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
    }


    @Pointcut("bean(*Controller)")
    private void allRequest() {}

    @Around("allRequest()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // requestUrl
        String requestUrl = request.getRequestURI();
        // requestMethod
        String requestMethod = request.getMethod();
        // userId
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() // 인증된 사용자의 이름 (주로 ID로 사용됨)
                : "Anonymous";
        // clientIp
        String clientIp = request.getRemoteAddr();

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
            ApiLog apiLog = ApiLog.builder().
                    requestUrl(requestUrl).
                    requestMethod(requestMethod).
                    userId(userId).
                    clientIp(clientIp).
                    responseStatus(responseStatus).
                    takenTime(timeTaken).
                    build();

            apiLogRepository.save(apiLog);

            log.info("[Request Log] requestUrl: {}, requestMethod: {}, userId: {}, clientIp: {}, pathVariable: {}, requestBody: {}, responseStatus: {}, timeTaken: {}",
                    requestUrl, requestMethod, userId, clientIp,
                    pathVariable != null ? pathVariable : "No Params",
                    requestBody != null ? requestBody : "No Body",
                    responseStatus, timeTaken);

            return result;

        } catch (Exception ex) {
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "allRequest()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Exception ex) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // requestUrl
        String requestUrl = request.getRequestURI();
        // requestMethod
        String requestMethod = request.getMethod();
        // userId
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName() // 인증된 사용자의 이름 (주로 ID로 사용됨)
                : "Anonymous";
        // clientIp
        String clientIp = request.getRemoteAddr();

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
                requestUrl, requestMethod, userId, clientIp, exceptionName, exceptionMessage, responseStatus);

        // DB에 에러 로그 저장
        ApiLog errorLog = ApiLog.builder().
                requestUrl(requestUrl).
                requestMethod(requestMethod).
                userId(userId).
                clientIp(clientIp).
                responseStatus(responseStatus).
                takenTime(0).
                build();
         // 상태 코드와 시간은 예제로 설정
        apiLogRepository.save(errorLog);
    }

    private int mapExceptionToStatusCode(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return 400; // Bad Request
        } else if (e instanceof org.springframework.security.access.AccessDeniedException) {
            return 403; // Forbidden
        } else if (e instanceof org.springframework.web.bind.MethodArgumentNotValidException) {
            return 422; // Unprocessable Entity
        } else {
            return 500; // Internal Server Error
        }
    }


}
