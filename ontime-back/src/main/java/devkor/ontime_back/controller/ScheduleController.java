package devkor.ontime_back.controller;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.ScheduleService;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;
    private final UserAuthService userAuthService;


    // 오늘의 약속 조회
    @Operation(summary = "사용자의 툭정 기간 일정 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "기간 JSON 데이터 (startDate와 endDate는 둘 다 선택 사항)"
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기간 스케줄 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\n \"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": [\n {\n \"scheduleId\": \"123e4567-e89b-12d3-a456-426614170105\",\n\"placeName\": \"My home\",\n\"scheduleName\": \"Family Meetings\",\n\"moveTime\": 20,\n\"scheduleTime\": \"2024-11-18T19:30:00\",\n\"scheduleSpareTime\": 15,\n\"scheduleNote\": \"Check project updates and next steps.\",\n\"latenessTime\": null\n },\n{\n\"scheduleId\": \"123e4567-e89b-12d3-a456-426614170106\",\n\"placeName\": \"My home\",\n\"scheduleName\": \"Family Meetingss\",\n\"moveTime\": 20,\n\"scheduleTime\": \"2024-11-18T19:30:00\",\n\"scheduleSpareTime\": 15,\n\"scheduleNote\": \"Check project updates and next steps.\",\n\"latenessTime\": null\n },\n{\n\"scheduleId\": \"123e4567-e89b-12d3-a456-426614170455\",\n\"placeName\": \"My home\",\n\"scheduleName\": \"Family Meetings\",\n\"moveTime\": 20,\n\"scheduleTime\": \"2024-11-18T19:30:00\",\n\"scheduleSpareTime\": 15,\n\"scheduleNote\": \"Check project updates and next steps.\",\n\"latenessTime\": null\n },\n "
                            )
                    )),
            @ApiResponse(responseCode = "4XX", description = "기간 내 스케줄을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"error\": \"No schedules found.\"}")
                    )
            )
    })
    @GetMapping("")
    public ResponseEntity<ApiResponseForm<List<ScheduleDto>>> getPeriodSchedule(HttpServletRequest request,
                                                                                @Parameter(description = "조회 시작 날짜 (ISO-8601 형식, 예: 2024-11-01T00:00:00)",
                                                                                        required = false,
                                                                                        example = "2024-11-15T18:00:00")
                                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                                                @Parameter(description = "조회 종료 날짜 (ISO-8601 형식, 예: 2024-11-01T00:00:00)",
                                                                                        required = false,
                                                                                        example = "2024-11-18T20:00:00")
                                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Long userId = userAuthService.getUserIdFromToken(request);
        List<ScheduleDto> schedules = scheduleService.showSchedulesByPeriod(userId, startDate, endDate);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(schedules));
    }

    // id로 스케줄 조회
    @Operation(summary = "일정 id로 일정 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "모든 일정 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨"
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\n \"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": [\n {\n \"scheduleId\": \"123e4567-e89b-12d3-a456-426614170105\",\n\"placeName\": \"My home\",\n\"scheduleName\": \"Family Meetings\",\n\"moveTime\": 20,\n\"scheduleTime\": \"2024-11-18T19:30:00\",\n\"scheduleSpareTime\": 15,\n\"scheduleNote\": \"Check project updates and next steps.\",\n\"latenessTime\": null\n },\n{\n\"scheduleId\": \"123e4567-e89b-12d3-a456-426614170106\",\n\"placeName\": \"My home\",\n\"scheduleName\": \"Family Meetingss\",\n\"moveTime\": 20,\n\"scheduleTime\": \"2024-11-18T19:30:00\",\n\"scheduleSpareTime\": 15,\n\"scheduleNote\": \"Check project updates and next steps.\",\n\"latenessTime\": null\n },\n{\n\"scheduleId\": \"123e4567-e89b-12d3-a456-426614170455\",\n\"placeName\": \"My home\",\n\"scheduleName\": \"Family Meetings\",\n\"moveTime\": 20,\n\"scheduleTime\": \"2024-11-18T19:30:00\",\n\"scheduleSpareTime\": 15,\n\"scheduleNote\": \"Check project updates and next steps.\",\n\"latenessTime\": null\n },\n "
                            )
                    )),
            @ApiResponse(responseCode = "4XX", description = "스케줄을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"error\": \"No schedules found.\"}")
                    )
            )
    })
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponseForm<ScheduleDto>> getScheduleById(
            HttpServletRequest request,
            @Parameter(description = "조회할 스케줄 ID (UUID 형식)",
                    required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afe5")
            @PathVariable UUID scheduleId) {

        Long userId = userAuthService.getUserIdFromToken(request);
        ScheduleDto schedule = scheduleService.showScheduleByScheduleId(userId, scheduleId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(schedule));
    }

    // 약속 삭제
    @Operation(summary = "사용자 일정 삭제",
            parameters = {
                    @Parameter(
                            name = "scheduleId",
                            description = "삭제할 일정의 ID",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afe5"
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 삭제 완료", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n\"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": null\n }"))),
            @ApiResponse(responseCode = "4XX", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponseForm<Void>> deleteSchedule(HttpServletRequest request, @PathVariable UUID scheduleId) {
        Long userId = userAuthService.getUserIdFromToken(request);
        scheduleService.deleteSchedule(scheduleId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(null));
    }

    // 약속 수정
    @Operation(
            summary = "사용자 일정 수정",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 수정 일정 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\n \"scheduleId\": \"3fa85f64-5717-4562-b3fc-2c963f66afe5\", \n \"placeId\": \"70d460da-6a82-4c57-a285-567cdeda5670\", \n \"placeName\": \"Home\", \n \"scheduleName\": \"Party\",\n \"moveTime\": 20,\n \"scheduleTime\": \"2024-11-15T19:30:00\",\n \"scheduleSpareTime\": 20,\n \"scheduleNote\": \"Write a message.\",\n \"latenessTime\": 0\n  }"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 수정 완료", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n\"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": null\n }"))),
            @ApiResponse(responseCode = "4XX", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponseForm<Void>> modifySchedule(HttpServletRequest request, @PathVariable UUID scheduleId, @RequestBody ScheduleModDto scheduleModDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        scheduleService.modifySchedule(userId, scheduleId, scheduleModDto);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(null));
    }

    // 약속 추가
    @Operation(
            summary = "사용자 일정 추가",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 일정 추가 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\n \"scheduleId\": \"3fa85f64-5717-4562-b3fc-2c963f66afe5\", \n \"placeId\": \"70d460da-6a82-4c57-a285-567cdeda5670\", \n \"placeName\": \"Home\", \n \"scheduleName\": \"Birthday Party\",\n \"moveTime\": 10,\n \"scheduleTime\": \"2024-11-15T19:30:00\",\n \"isChange\": false\n, \n \"isStarted\": false\n, \n \"scheduleSpareTime\": 20,\n \"scheduleNote\": \"Write a message.\"  }"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 추가 완료", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n\"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": null\n }"))),
            @ApiResponse(responseCode = "4XX", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("")
    public ResponseEntity<ApiResponseForm<Void>> addSchedule(HttpServletRequest request, @RequestBody ScheduleAddDto scheduleAddDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        scheduleService.addSchedule(scheduleAddDto, userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(null));
    }

    @Operation(
            summary = "지각 히스토리 조회 (지각시간이 0초과인 약속들 조회)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "지각 히스토리 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 넣으면 됨.",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "지각 히스토리 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"지각 히스토리 조회 성공!\",\n  \"data\": [\n    {\n      \"scheduleId\": \"3fa85f64-5717-4562-b3fc-2c963f66afe5\",\n      \"scheduleName\": \"BirthDay Party\",\n      \"scheduleTime\": \"2024-12-23T19:30:00\",\n      \"latenessTime\": 12\n    },\n    {\n      \"scheduleId\": \"3fa85f64-5717-4562-b3fc-2c963f66afe6\",\n      \"scheduleName\": \"Chirstmas Party\",\n      \"scheduleTime\": \"2024-12-25T19:30:00\",\n      \"latenessTime\": 5\n    }\n  ]\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "지각 히스토리 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(토큰 오류 제외 비즈니스 로직 오류는 없음)")))
    })
    @GetMapping("/lateness-history") // 지각 히스토리 조회
    public ResponseEntity<ApiResponseForm<List<LatenessHistoryResponse>>> getLatenessHistory(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        List<LatenessHistoryResponse> latenessHistory = scheduleService.getLatenessHistory(userId);
        String message = "지각 히스토리 조회 성공!";
        return ResponseEntity.ok(ApiResponseForm.success(latenessHistory, message));
    }

    @Operation(summary = "준비과정 조회",
            parameters = {
                    @Parameter(
                            name = "scheduleId",
                            description = "확인할 일정의 ID",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "준비과정 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "[\n {\n \"preparationId\": \"123e4567-e89b-12d3-a456-426614174011\",\n \"preparationName\": \"Step 1: Wake up\",\n \"preparationTime\": 5,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174012\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174012\",\n\"preparationName\": \"Step 2: Brush teeth\",\n \"preparationTime\": 15,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174013\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174013\",\n\"preparationName\": \"Step 3: Wearing Clothes\",\n\"preparationTime\": 15,\n\"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174014\"\n },\n{\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174014\",\n\"preparationName\": \"Step 4: Breakfast\",\n\"preparationTime\": 30,\n\"nextPreparationId\": null\n }\n ]"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "준비과정 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("/{scheduleId}/preparation")
    public ResponseEntity<ApiResponseForm<List<PreparationDto>>> getPreparation(HttpServletRequest request, @PathVariable UUID scheduleId) {
        Long userId = userAuthService.getUserIdFromToken(request);
        List<PreparationDto> preparationDtoList = scheduleService.getPreparations(userId, scheduleId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(preparationDtoList));
    }




    @Operation(
            summary = "약속 준비 종료 이후 지각시간, 성실도점수 업데이트 (약속 준비 종료 이후 호출해야함)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "성실도 점수 초기화 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"scheduleId\": \"3fa85f64-5717-4562-b3fc-2c963f66afe5\", \"latenessTime\": 3}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "지각시간, 성실도점수 업데이트 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"지각시간과 성실도점수가 성공적으로 업데이트 되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "지각시간, 성실도점수 업데이트 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(스케줄ID에 해당하는 약속이 없을 때 Schedule Not Found 메세지 반환)")))
    })
    @PutMapping("/finish") // 약속 준비 종료 이후 지각시간(Schedule 테이블), 성실도 점수(User 테이블) 업데이트
    public ResponseEntity<ApiResponseForm<?>> finishSchedule(
            HttpServletRequest request,
            @RequestBody FinishPreparationDto finishPreparationDto) {

        Long userId = userAuthService.getUserIdFromToken(request);
        scheduleService.finishSchedule(userId, finishPreparationDto);
        String message = "지각시간과 성실도점수가 성공적으로 업데이트 되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }


}

