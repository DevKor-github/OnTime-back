package devkor.ontime_back.controller;

import devkor.ontime_back.dto.FeedbackAddDto;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.FeedbackService;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final UserAuthService userAuthService;

    @Operation(
            summary = "피드백 저장 (사용자 -> 운영자 피드백 저장)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "피드백 저장 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"feedbackId\": \"d784cde3-9ff9-4054-872a-500bbcc2198a\", \"message\": \"피드백입니다. 이런게 아쉬워요\"}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "피드백 저장 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"피드백이 성공적으로 저장되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "피드백 저장 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("")
    public ResponseEntity<ApiResponseForm<?>> saveFeedback(HttpServletRequest request, @RequestBody FeedbackAddDto feedbackAddDto) {
        Long userId = userAuthService.getUserIdFromToken(request);

        feedbackService.saveFeedback(userId, feedbackAddDto);

        String message = "피드백이 성공적으로 저장되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }
}