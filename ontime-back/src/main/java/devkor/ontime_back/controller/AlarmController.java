package devkor.ontime_back.controller;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.AlarmService;
import devkor.ontime_back.service.UserAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AlarmController {

    private final UserAuthService userAuthService;
    private final AlarmService alarmService;

    @GetMapping("/users/me/alarm-settings")
    public ResponseEntity<ApiResponseForm<AlarmSettingsResponseDto>> getAlarmSettings(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.getAlarmSettings(userId)));
    }

    @PatchMapping("/users/me/alarm-settings")
    public ResponseEntity<ApiResponseForm<AlarmSettingsResponseDto>> patchAlarmSettings(
            HttpServletRequest request,
            @RequestBody Map<String, Object> requestBody) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.patchAlarmSettings(userId, requestBody)));
    }

    @PutMapping("/users/me/devices/current")
    public ResponseEntity<ApiResponseForm<AlarmDeviceCurrentResponseDto>> registerCurrentDevice(
            HttpServletRequest request,
            @RequestBody AlarmDeviceCurrentRequestDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.registerCurrentDevice(
                        userId,
                        requestDto,
                        userAuthService.getAccessTokenFromRequest(request),
                        userAuthService.getRefreshTokenFromRequest(request))));
    }

    @DeleteMapping("/users/me/devices/current")
    public ResponseEntity<ApiResponseForm<AlarmDeviceUnregisterResponseDto>> unregisterCurrentDevice(
            HttpServletRequest request,
            @RequestBody(required = false) AlarmDeviceUnregisterRequestDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.unregisterCurrentDevice(
                        userId,
                        requestDto,
                        userAuthService.getAccessTokenFromRequest(request))));
    }

    @PostMapping("/users/me/alarm-status")
    public ResponseEntity<ApiResponseForm<AlarmStatusReportResponseDto>> reportAlarmStatus(
            HttpServletRequest request,
            @RequestBody AlarmStatusReportRequestDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.reportAlarmStatus(
                        userId,
                        requestDto,
                        userAuthService.getAccessTokenFromRequest(request))));
    }

    @GetMapping("/users/me/alarm-status")
    public ResponseEntity<ApiResponseForm<AlarmStatusCurrentResponseDto>> getCurrentAlarmStatus(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.getCurrentAlarmStatus(
                        userId,
                        userAuthService.getAccessTokenFromRequest(request))));
    }
}
