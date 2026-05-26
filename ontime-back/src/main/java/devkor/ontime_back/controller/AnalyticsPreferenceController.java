package devkor.ontime_back.controller;

import devkor.ontime_back.dto.AnalyticsPreferenceResponseDto;
import devkor.ontime_back.dto.AnalyticsPreferenceUpdateDto;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.AnalyticsPreferenceService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalyticsPreferenceController {

    private final UserAuthService userAuthService;
    private final AnalyticsPreferenceService analyticsPreferenceService;

    @Operation(summary = "Get current user's analytics preference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics preference lookup succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"enabled\": false,\n    \"updatedAt\": \"2026-05-26T12:00:00Z\"\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Analytics preference lookup failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @GetMapping("/users/me/analytics-preference")
    public ResponseEntity<ApiResponseForm<AnalyticsPreferenceResponseDto>> getAnalyticsPreference(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(analyticsPreferenceService.getAnalyticsPreference(userId)));
    }

    @Operation(
            summary = "Update current user's analytics preference",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Account-scoped analytics preference update.",
                    required = true,
                    content = @Content(schema = @Schema(
                            type = "object",
                            example = "{\"enabled\": false}"
                    ))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics preference update succeeded", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\n  \"status\": \"success\",\n  \"code\": 200,\n  \"message\": \"OK\",\n  \"data\": {\n    \"enabled\": false,\n    \"updatedAt\": \"2026-05-26T12:00:05Z\"\n  }\n}")
            )),
            @ApiResponse(responseCode = "4XX", description = "Analytics preference update failed", content = @Content(mediaType = "application/json", schema = @Schema(example = "Failure message")))
    })
    @PutMapping("/users/me/analytics-preference")
    public ResponseEntity<ApiResponseForm<AnalyticsPreferenceResponseDto>> updateAnalyticsPreference(
            HttpServletRequest request,
            @Valid @RequestBody AnalyticsPreferenceUpdateDto requestDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseForm.success(analyticsPreferenceService.updateAnalyticsPreference(userId, requestDto)));
    }
}
