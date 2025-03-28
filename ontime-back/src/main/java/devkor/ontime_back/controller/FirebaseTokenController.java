package devkor.ontime_back.controller;

import devkor.ontime_back.dto.FirebaseTokenAddDto;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.FirebaseTokenService;
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
@RequestMapping("/firebase-token")
@RequiredArgsConstructor
public class FirebaseTokenController {
    private final UserAuthService userAuthService;
    private final FirebaseTokenService firebaseTokenService;

    @Operation(
            summary = "FCM 토큰 User테이블에 저장",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "FCM 토큰 저장 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"firebaseToken\": \"token1234abcd(실제로는 firebase에서 받은 토큰을 기입해야 함)\"}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FCM 토큰 저장 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"FCM 토큰이 성공적으로 User테이블에 저장되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "FCM 토큰 저장 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("")
    public ResponseEntity<ApiResponseForm<String>> registerFirebaseToken(HttpServletRequest request, @RequestBody FirebaseTokenAddDto firebaseTokenAddDto) {
        Long userId = userAuthService.getUserIdFromToken(request);

        firebaseTokenService.registerFirebaseToken(userId, firebaseTokenAddDto);

        String message = "FCM 토큰이 성공적으로 User테이블에 저장되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }


    @Operation(
            summary = "FCM 푸시 테스트",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "FCM 푸시 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FCM 푸시 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"성공적으로 FCM 푸시가 되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "FCM 푸시 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("/push-test")
    public ResponseEntity<ApiResponseForm<String>> sendTestNotification(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);

        firebaseTokenService.sendTestNotification(userId);

        String message = "Firebase 푸시 메세지가 성공적으로 Firebase에 전달되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }
}
