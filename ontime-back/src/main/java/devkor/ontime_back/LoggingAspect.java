package devkor.ontime_back;

import devkor.ontime_back.dto.RequestInfoDto;
import devkor.ontime_back.entity.ApiLog;
import devkor.ontime_back.logging.RequestLogPolicy;
import devkor.ontime_back.response.GeneralException;
import devkor.ontime_back.service.ApiLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    private final ApiLogService apiLogService;

    @Pointcut("bean(*Controller)")
    private void allRequest() {}

    @Around("allRequest()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String requestId = RequestLogPolicy.resolveRequestId(request);
        RequestLogPolicy.exposeRequestId(attributes, requestId);
        RequestInfoDto requestInfoDto = extractRequestInfo(request);
        long beforeRequest = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            int responseStatus = 200;
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                responseStatus = responseEntity.getStatusCode().value();
            }

            long timeTaken = System.currentTimeMillis() - beforeRequest;
            saveApiLog(requestInfoDto, responseStatus, timeTaken);
            log.info("[Request Log] requestId: {}, route: {}, method: {}, actor: {}, clientIp: {}, responseStatus: {}, timeTakenMs: {}",
                    requestId, requestInfoDto.getRequestUrl(), requestInfoDto.getRequestMethod(), requestInfoDto.getUserId(),
                    requestInfoDto.getClientIp(), responseStatus, timeTaken);

            return result;
        } catch (Throwable ex) {
            int responseStatus = mapExceptionToStatusCode(ex);
            long timeTaken = System.currentTimeMillis() - beforeRequest;
            saveApiLog(requestInfoDto, responseStatus, timeTaken);
            log.error("[Error Log] requestId: {}, route: {}, method: {}, actor: {}, clientIp: {}, exception: {}, responseStatus: {}, timeTakenMs: {}",
                    requestId, requestInfoDto.getRequestUrl(), requestInfoDto.getRequestMethod(), requestInfoDto.getUserId(),
                    requestInfoDto.getClientIp(), ex.getClass().getSimpleName(), responseStatus, timeTaken);
            throw ex;
        }
    }

    // requestinfo 추출
    private RequestInfoDto extractRequestInfo(HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : "Anonymous";
        String clientIp = request.getRemoteAddr();

        return new RequestInfoDto(requestUrl, requestMethod, userId, clientIp);
    }

    private void saveApiLog(RequestInfoDto requestInfoDto, int responseStatus, long timeTaken) {
        ApiLog apiLog = buildApiLog(requestInfoDto, responseStatus, timeTaken);
        apiLogService.saveLog(apiLog);
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

    private int mapExceptionToStatusCode(Throwable e) {
        if (e instanceof GeneralException ge) {
            return ge.getErrorCode().getCode();
        }
        return 500;
    }

}
