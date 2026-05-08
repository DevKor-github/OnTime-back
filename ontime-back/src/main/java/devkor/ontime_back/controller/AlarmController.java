package devkor.ontime_back.controller;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.AlarmService;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AlarmController {

    private final UserAuthService userAuthService;
    private final AlarmService alarmService;

    @Operation(summary = "Get current user's native alarm settings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alarm settings lookup succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"alarmsEnabled\": true,\n    \"defaultAlarmOffsetMinutes\": 10,\n    \"updatedAt\": \"2026-05-05T00:00:00Z\"\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Alarm settings lookup failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @GetMapping("/users/me/alarm-settings")
    public ResponseEntity<ApiResponseForm<AlarmSettingsResponseDto>> getAlarmSettings(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.getAlarmSettings(userId)));
    }

    @Operation(
            summary = "Update current user's native alarm settings",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Partial alarm settings update. Omit fields that should not change.",
                    required = true,
                    content = @Content(schema = @Schema(
                            type = "object",
                            example = "{\"alarmsEnabled\": true, \"defaultAlarmOffsetMinutes\": 10}"
                    ))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alarm settings update succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"alarmsEnabled\": true,\n    \"defaultAlarmOffsetMinutes\": 10,\n    \"updatedAt\": \"2026-05-05T00:00:00Z\"\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Alarm settings update failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @PatchMapping("/users/me/alarm-settings")
    public ResponseEntity<ApiResponseForm<AlarmSettingsResponseDto>> patchAlarmSettings(
            HttpServletRequest request,
            @Valid @RequestBody AlarmSettingsPatchDto requestBody) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.patchAlarmSettings(userId, requestBody)));
    }

    @Operation(
            summary = "Register current device for native alarm ownership",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Current device metadata used to bind the logged-in access token to native alarm status.",
                    required = true,
                    content = @Content(schema = @Schema(
                            type = "object",
                            example = "{\"deviceId\": \"ios-device-000001\", \"platform\": \"ios\", \"appVersion\": \"1.2.3\", \"osVersion\": \"iOS 18.0\", \"supportsNativeAlarm\": true, \"nativeAlarmProvider\": \"iosAlarmKit\", \"fallbackProvider\": \"localNotification\"}"
                    ))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current device registration succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"deviceId\": \"ios-device-000001\",\n    \"active\": true,\n    \"lastSeenAt\": \"2026-05-05T00:00:00Z\"\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Current device registration failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @PutMapping("/users/me/devices/current")
    public ResponseEntity<ApiResponseForm<AlarmDeviceCurrentResponseDto>> registerCurrentDevice(
            HttpServletRequest request,
            @Valid @RequestBody AlarmDeviceCurrentRequestDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.registerCurrentDevice(
                        userId,
                        requestDto,
                        userAuthService.getAccessTokenFromRequest(request),
                        userAuthService.getRefreshTokenFromRequest(request))));
    }

    @Operation(
            summary = "Unregister current device from native alarm ownership",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Optional device ID. If omitted, the device bound to the access token is unregistered.",
                    content = @Content(schema = @Schema(
                            type = "object",
                            example = "{\"deviceId\": \"ios-device-000001\"}"
                    ))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current device unregister succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"active\": false\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Current device unregister failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @DeleteMapping("/users/me/devices/current")
    public ResponseEntity<ApiResponseForm<AlarmDeviceUnregisterResponseDto>> unregisterCurrentDevice(
            HttpServletRequest request,
            @Valid @RequestBody(required = false) AlarmDeviceUnregisterRequestDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.unregisterCurrentDevice(
                        userId,
                        requestDto,
                        userAuthService.getAccessTokenFromRequest(request))));
    }

    @Operation(
            summary = "Report current native alarm reconciliation status",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Native alarm reconciliation status for the current device and schedule window.",
                    required = true,
                    content = @Content(schema = @Schema(
                            type = "object",
                            example = "{\"deviceId\": \"ios-device-000001\", \"reconciledAt\": \"2026-05-05T09:00:00+09:00\", \"scheduleWindowStart\": \"2026-05-05T00:00:00\", \"scheduleWindowEnd\": \"2026-05-06T00:00:00\", \"alarmCoverageStart\": \"2026-05-05T00:00:00\", \"alarmCoverageEnd\": \"2026-05-06T00:00:00\", \"status\": \"armed\", \"permissionIssue\": null, \"nativeAlarmProvider\": \"iosAlarmKit\", \"fallbackProvider\": \"localNotification\", \"armedScheduleCount\": 1, \"armedScheduleIds\": [\"3fa85f64-5717-4562-b3fc-2c963f66afe5\"], \"skippedScheduleCount\": 0, \"failures\": []}"
                    ))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alarm status report succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"received\": true\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Alarm status report failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @PostMapping("/users/me/alarm-status")
    public ResponseEntity<ApiResponseForm<AlarmStatusReportResponseDto>> reportAlarmStatus(
            HttpServletRequest request,
            @Valid @RequestBody AlarmStatusReportRequestDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.reportAlarmStatus(
                        userId,
                        requestDto,
                        userAuthService.getAccessTokenFromRequest(request))));
    }

    @Operation(summary = "Get current native alarm reconciliation status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current alarm status lookup succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"deviceId\": \"ios-device-000001\",\n    \"active\": true,\n    \"platform\": \"ios\",\n    \"appVersion\": \"1.2.3\",\n    \"osVersion\": \"iOS 18.0\",\n    \"supportsNativeAlarm\": true,\n    \"nativeAlarmProvider\": \"iosAlarmKit\",\n    \"fallbackProvider\": \"localNotification\",\n    \"lastSeenAt\": \"2026-05-05T00:00:00Z\",\n    \"reconciledAt\": \"2026-05-05T00:00:00Z\",\n    \"scheduleWindowStart\": \"2026-05-05T00:00:00\",\n    \"scheduleWindowEnd\": \"2026-05-06T00:00:00\",\n    \"alarmCoverageStart\": \"2026-05-05T00:00:00\",\n    \"alarmCoverageEnd\": \"2026-05-06T00:00:00\",\n    \"status\": \"armed\",\n    \"permissionIssue\": null,\n    \"armedScheduleCount\": 1,\n    \"armedScheduleIds\": [\"3fa85f64-5717-4562-b3fc-2c963f66afe5\"],\n    \"skippedScheduleCount\": 0,\n    \"failures\": [],\n    \"updatedAt\": \"2026-05-05T00:00:00Z\"\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Current alarm status lookup failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @GetMapping("/users/me/alarm-status")
    public ResponseEntity<ApiResponseForm<AlarmStatusCurrentResponseDto>> getCurrentAlarmStatus(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(alarmService.getCurrentAlarmStatus(
                        userId,
                        userAuthService.getAccessTokenFromRequest(request))));
    }
}
