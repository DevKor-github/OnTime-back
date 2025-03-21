package devkor.ontime_back.controller;

import devkor.ontime_back.dto.PreparationDto;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.PreparationUserService;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/preparation-users")
@RequiredArgsConstructor
public class PreparationUserController {


    private final PreparationUserService preparationUserService;
    private final UserAuthService userAuthService;

    @Operation(
            summary = "사용자 준비과정 수정",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 준비과정 관련 JSON 데이터",
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
            @ApiResponse(responseCode = "200", description = "사용자 준비과정 수정 완료", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n\"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\": null\n }"))),
            @ApiResponse(responseCode = "4XX", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PutMapping("")
    public ResponseEntity<ApiResponseForm<Void>> modifyPreparationUser(HttpServletRequest request, @RequestBody List<PreparationDto> preparationDtoList) {
        Long userId = userAuthService.getUserIdFromToken(request);

        preparationUserService.updatePreparationUsers(userId, preparationDtoList);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(null));
    }

    @Operation(summary = "사용자 준비과정 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "모든 일정 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨"
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "준비과정 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n \"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"OK\",\n \"data\":[\n {\n \"preparationId\": \"123e4567-e89b-12d3-a456-426614174011\",\n \"preparationName\": \"Step 1: Wake up\",\n \"preparationTime\": 5,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174012\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174012\",\n\"preparationName\": \"Step 2: Brush teeth\",\n \"preparationTime\": 15,\n \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174013\"\n },\n {\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174013\",\n\"preparationName\": \"Step 3: Wearing Clothes\",\n\"preparationTime\": 15,\n\"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174014\"\n },\n{\n\"preparationId\": \"123e4567-e89b-12d3-a456-426614174014\",\n\"preparationName\": \"Step 4: Breakfast\",\n\"preparationTime\": 30,\n\"nextPreparationId\": null\n }\n ] }\n"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "준비과정 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("")
    public ResponseEntity<ApiResponseForm<List<PreparationDto>>> getAllPreparationUser(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);

        List<PreparationDto> preparationUserList = preparationUserService.showAllPreparationUsers(userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseForm.success(preparationUserList));

    }

}
