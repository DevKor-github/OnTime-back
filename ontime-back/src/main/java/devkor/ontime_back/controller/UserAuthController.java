package devkor.ontime_back.controller;

import devkor.ontime_back.dto.ChangePasswordDto;
import devkor.ontime_back.dto.UserInfoResponse;
import devkor.ontime_back.dto.UserSignUpDto;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.repository.UserRepository;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @Operation(
            summary = "일반 회원가입 (회원가입 시 자동으로 로그인도 되어 헤더에 JWT토큰을 반환함)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원가입 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(type = "object", example = "{\"email\": \"user@example.com\", \"password\": \"password123\", \"name\": \"junbeom\"}")
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"회원가입이 성공적으로 완료되었습니다. 온보딩을 진행해주세요( /user/onboarding )\",\n  \"data\": {\n    \"userId\": 1\n  }}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "회원가입 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(이메일이 이미 존재할 경우, 이름이 이미 존재할 경우 다르게 출력)")))
    })
    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponseForm<UserInfoResponse>> signUp(HttpServletRequest request, HttpServletResponse response, @RequestBody UserSignUpDto userSignUpDto) throws Exception {
        UserInfoResponse userSignUpResponse = userAuthService.signUp(request, response, userSignUpDto);
        String message = "회원가입이 성공적으로 완료되었습니다. 온보딩을 진행해주세요( /user/onboarding )";
        return ResponseEntity.ok(ApiResponseForm.success(userSignUpResponse, message));
    }


    @Operation(summary = "일반 로그인 (로그인 요청을 통해 JWT 토큰을 발급받음)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일반 로그인 성공(반환 문자열 없음. 헤더에 토큰 반환)", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\n  \"message\": \"유저의 ROLE이 GUEST이므로 온보딩API를 호출해 온보딩을 진행해야합니다.\", \"role\": \"GUEST\"}"))),
            @ApiResponse(responseCode = "4XX", description = "일반 로그인 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("/login")
    public String login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그인 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(type = "object", example = "{\"email\": \"user@example.com\", \"password\": \"password123\"}")
                    )
            )
            @RequestBody Map<String, String> loginRequest) {
        return "로그인 성공"; // 실제 로그인 처리는 Security 필터에서 수행
    }


    @Operation(
            summary = "비밀번호 변경",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "비밀번호 변경 요청 JSON 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"currentPassword\": \"password123\", \"newPassword\": \"1q2w3e4r!\"}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"비밀번호가 성공적으로 변경되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "비밀번호 변경 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(기존 비밀번호가 틀린경우, 기존 비밀번호와 새 비밀번호가 같은 경우 다르게 출력)")))
    })
    @PutMapping("users/me/password")
    public ResponseEntity<ApiResponseForm<String>> changePassword(HttpServletRequest request, @RequestBody ChangePasswordDto changePasswordDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        userAuthService.changePassword(userId, changePasswordDto);
        String message = "비밀번호가 성공적으로 변경되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }


    @Operation(
            summary = "계정 삭제 (User 데이터 하드 삭제)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "계정 삭제 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "계정 삭제 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"계정이 성공적으로 삭제되었습니다!\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "계정 삭제 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(토큰 오류 제외 비즈니스 로직 오류는 없음)")))
    })
    @DeleteMapping("/users/me/delete")
    public ResponseEntity<ApiResponseForm<?>> deleteUser(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);
        userAuthService.deleteUser(userId);
        String message = "계정이 성공적으로 삭제되었습니다!";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }

}
