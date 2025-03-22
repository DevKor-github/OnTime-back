package devkor.ontime_back.controller;

import devkor.ontime_back.dto.PunctualityScoreResponse;
import devkor.ontime_back.dto.UpdateSpareTimeDto;
import devkor.ontime_back.dto.UserInfoResponse;
import devkor.ontime_back.dto.UserOnboardingDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.UserAuthService;
import devkor.ontime_back.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAuthService userAuthService;

    @Operation(
            summary = "성실도 점수 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "성실도 점수 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성실도 점수 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"-1이면 성실도점수 초기화 직후의 상태. 0~100의 float 자료형이면 성실도 점수\",\n  \"data\": {\n    \"punctualityScore\": -1\n  }}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "성실도 점수 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("/me/punctuality-score") // 성실도 점수 조회
    public ResponseEntity<ApiResponseForm<PunctualityScoreResponse>> getPunctualityScore(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        float punctualityScore = userService.getPunctualityScore(userId); // -1 or float 0~100 반환
        String message = "-1이면 성실도점수 초기화 직후의 상태. 0~100의 float 자료형이면 성실도 점수";
        return ResponseEntity.ok(ApiResponseForm.success(new PunctualityScoreResponse(punctualityScore), message));
    }


    @Operation(
            summary = "성실도 점수 초기화",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "성실도 점수 초기화 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성실도 점수 초기회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"성실도 점수가 성공적으로 초기화 되었습니다! (초기화 이후 약속 수 <- 0, 초기화 이후 지각 수 <- 0, 성실도 점수 <- -1)\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "성실도 점수 초기화 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PutMapping("/me/punctuality-score") // 성실도 점수 초기화
    public ResponseEntity<ApiResponseForm<String>> resetPunctualityScore(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        userService.resetPunctualityScore(userId);
        String message = "성실도 점수가 성공적으로 초기화 되었습니다! (초기화 이후 약속 수 <- 0, 초기화 이후 지각 수 <- 0, 성실도 점수 <- -1)";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }


    @Operation(
            summary = "사용자 여유시간 업데이트",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 여유시간 업데이트 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"newSpareTime\": 30}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 여유시간 업데이트 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"사용자 여유시간이 성공적으로 업데이트되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "사용자 여유시간 업데이트 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PutMapping("/me/spare-time")
    public ResponseEntity<ApiResponseForm<?>> updateSetting(HttpServletRequest request, @RequestBody UpdateSpareTimeDto updateSpareTimeDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        userService.updateSpareTime(userId, updateSpareTimeDto);
        String message = "사용자 여유시간이 성공적으로 업데이트되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }


    @Operation(
            summary = "온보딩 (여유시간, 잊으면 안될 것들, 준비과정 첫 세팅)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "온보딩 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\n  \"spareTime\": 30,\n  \"note\": \"내 인생에 지각은 없다!!!\",\n  \"preparationList\": [\n    {\n      \"preparationId\": \"123e4567-e89b-12d3-a456-426614174011\",\n      \"preparationName\": \"기상하기\",\n      \"preparationTime\": 10,\n      \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174012\"\n    },\n    {\n      \"preparationId\": \"123e4567-e89b-12d3-a456-426614174012\",\n      \"preparationName\": \"세수하기\",\n      \"preparationTime\": 10,\n      \"nextPreparationId\": \"123e4567-e89b-12d3-a456-426614174013\"\n    },\n    {\n      \"preparationId\": \"123e4567-e89b-12d3-a456-426614174013\",\n      \"preparationName\": \"화장하기\",\n      \"preparationTime\": 10,\n      \"nextPreparationId\": null\n    }\n  ]\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "온보딩 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"온보딩이 성공적으로 완료되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "온보딩 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(토큰 오류 제외 비즈니스 로직 오류는 없음)")))
    })
    @PutMapping("/me/onboarding")
    public ResponseEntity<ApiResponseForm<?>> addInfo(HttpServletRequest request, @RequestBody UserOnboardingDto userOnboardingDto) throws Exception {
        Long userId = userAuthService.getUserIdFromToken(request);
        userService.onboarding(userId, userOnboardingDto);
        String message = "온보딩이 성공적으로 완료되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }

    @Operation(
            summary = "사용자 정보 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 정보 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"사용자 정보 조회 성공\",\n  \"data\": {\n    \"userId\": 1,\n    \"email\": \"user@example.com\",\n    \"name\": \"junbeom\",\n    \"spareTime\": 30,\n    \"note\": \"내 인생에 지각은 없다!!!\",\n    \"punctualityScore\": -1,\n    \"role\": \"USER\"\n  }\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "사용자 정보 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponseForm<UserInfoResponse>> getUserInfo(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        User user = userService.getUserInfo(userId);
        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .spareTime(user.getSpareTime())
                .note(user.getNote())
                .punctualityScore(user.getPunctualityScore())
                .build();
        String message = "사용자 정보 조회 성공";
        return ResponseEntity.ok(ApiResponseForm.success(userInfoResponse, message));
    }

}

