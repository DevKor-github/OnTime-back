package devkor.ontime_back.controller;

import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.PreparationScheduleService;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/preparationschedule")
@RequiredArgsConstructor
public class PreparationScheduleController {

    private final PreparationScheduleService preparationScheduleService;
    private final UserAuthService userAuthService;

    @Operation(
            summary = "스케줄별 준비과정 생성",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "스케줄 준비과정 관련 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "[\n {\n \"preparationId\": \"123e4567-e89b-12d3-a456-426614174011\",\n \"preparationName\": \"Step 1: Wake up\",\n \"preparationTime\": 5,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174012\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174012\",\n\"preparationName\": \"Step 2: Brush teeth\",\n \"preparationTime\": 15,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174013\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174013\",\n\"preparationName\": \"Step 3: Wearing Clothes\",\n\"preparationTime\": 15,\n\"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174014\"\n },\n{\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174014\",\n\"preparationName\": \"Step 4: Breakfast\",\n\"preparationTime\": 30,\n\"nextPreparationId\": null\n }\n ]"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄 준비과정 생성 완료", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n\"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": null\n }"))),
            @ApiResponse(responseCode = "4XX", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("/create/{scheduleId}")
    public ResponseEntity<ApiResponseForm<Void>> createPreparationSchedule(HttpServletRequest request, @Parameter(description = "스케줄 ID (UUID 형식)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afe5") @PathVariable UUID scheduleId, @RequestBody List<PreparationDto> preparationDtoList) {
        Long userId = userAuthService.getUserIdFromToken(request);

        preparationScheduleService.makePreparationSchedules(userId, scheduleId, preparationDtoList);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(null));
    }

    @Operation(
            summary = "스케줄별 준비과정 수정",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "스케줄 준비과정 관련 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "[\n {\n \"preparationId\": \"123e4567-e89b-12d3-a456-426614174011\",\n \"preparationName\": \"Step 1: Wake up\",\n \"preparationTime\": 5,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174012\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174012\",\n\"preparationName\": \"Step 2: Brush teeth\",\n \"preparationTime\": 15,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174013\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174013\",\n\"preparationName\": \"Step 3: Wearing Clothes\",\n\"preparationTime\": 15,\n\"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174014\"\n },\n{\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174014\",\n\"preparationName\": \"Step 4: Breakfast\",\n\"preparationTime\": 30,\n\"nextPreparationId\": null\n }\n ]"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄 준비과정 수정 완료", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n\"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": null\n }"))),
            @ApiResponse(responseCode = "4XX", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("/modify/{scheduleId}")
    public ResponseEntity<ApiResponseForm<Void>> modifyPreparationSchedule(HttpServletRequest request, @Parameter(description = "스케줄 ID (UUID 형식)", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afe5") @PathVariable UUID scheduleId, @RequestBody List<PreparationDto> preparationDtoList) {
        Long userId = userAuthService.getUserIdFromToken(request);

        preparationScheduleService.updatePreparationSchedules(userId, scheduleId, preparationDtoList);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(null));
    }

}
