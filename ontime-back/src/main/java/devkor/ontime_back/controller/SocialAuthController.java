package devkor.ontime_back.controller;

import devkor.ontime_back.dto.FeedbackAddDto;
import devkor.ontime_back.dto.OAuthAppleRequestDto;
import devkor.ontime_back.dto.OAuthGoogleRequestDto;
import devkor.ontime_back.dto.OAuthKakaoUserDto;
import devkor.ontime_back.global.oauth.apple.AppleLoginService;
import devkor.ontime_back.global.oauth.google.GoogleLoginService;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class SocialAuthController {

    private final UserAuthService userAuthService;
    private final AppleLoginService appleLoginService;
    private final GoogleLoginService googleLoginService;

    @Operation(
            summary = "구글 소셜 로그인/회원가입",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "구글 identity token 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = OAuthGoogleRequestDto.class,
                                    example = "{\n  \"idToken\": \"eyJhbGxxxxxxx\",\n  \"refreshToken\": \"google-refresh-token\"\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "구글 로그인/회원가입 성공. 토큰은 응답 헤더에 반환됨", headers = {
                    @Header(name = "Authorization", description = "Bearer access token"),
                    @Header(name = "Authorization-refresh", description = "Bearer refresh token")
            }, content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"로그인에 성공하였습니다.\",\n  \"data\": {\n    \"userId\": 1,\n    \"email\": \"user@example.com\",\n    \"name\": \"junbeom\",\n    \"spareTime\": 10,\n    \"note\": null,\n    \"punctualityScore\": 100.0,\n    \"role\": \"USER\"\n  }\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(이메일이 이미 존재할 경우, 이름이 이미 존재할 경우 다르게 출력)")))
    })
    @PostMapping("/google/login")
    public String googleRegisterOrLogin(@Valid @RequestBody OAuthGoogleRequestDto oAuthGoogleRequestDto, HttpServletResponse response) {
        return "구글 로그인/회원가입 성공"; // 로그인 처리는 필터에서 적용
    }

    @Operation(
            summary = "카카오 소셜 로그인/회원가입",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "카카오 회원정보 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = OAuthKakaoUserDto.class,
                                    example = "{\n  \"id\": \"4803687123\",\n  \"profile\": {\n    \"nickname\": \"김철수\",\n    \"thumbnailImageUrl\": \"https://example.com/thumb.jpg\",\n    \"profile_image_url\": \"https://example.com/profile.jpg\",\n    \"defaultImage\": false,\n    \"defaultNickname\": false\n  }\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카카오 로그인/회원가입 성공. 토큰은 응답 헤더에 반환됨", headers = {
                    @Header(name = "Authorization", description = "Bearer access token"),
                    @Header(name = "Authorization-refresh", description = "Bearer refresh token")
            }, content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"message\": \"유저의 ROLE이 GUEST이므로 온보딩API를 호출해 온보딩을 진행해야합니다.\",\n  \"role\": \"GUEST\"}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(이메일이 이미 존재할 경우, 이름이 이미 존재할 경우 다르게 출력)")))
    })
    @PostMapping("/kakao/login")
    public String kakaoRegisterOrLogin(@Valid @RequestBody OAuthKakaoUserDto oAuthKakaoUserDto, HttpServletResponse response) {
        return "카카오 로그인/회원가입 성공"; // 로그인 처리는 필터에서 적용
    }

    @Operation(
            summary = "애플 소셜 로그인/회원가입",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "애플 idtoken, authcode, fullname",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = OAuthAppleRequestDto.class,
                                    example = "{\n \"idToken\": \".\",\n  \"authCode\": \".\",\n  \"fullName\": \"허진서\" }"                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "애플 로그인/회원가입 성공. 토큰은 응답 헤더에 반환됨", headers = {
                    @Header(name = "Authorization", description = "Bearer access token"),
                    @Header(name = "Authorization-refresh", description = "Bearer refresh token")
            }, content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"로그인에 성공하였습니다.\",\n  \"data\": {\n    \"userId\": 1,\n    \"email\": \"user@example.com\",\n    \"name\": \"junbeom\",\n    \"spareTime\": 10,\n    \"note\": null,\n    \"punctualityScore\": 100.0,\n    \"role\": \"USER\"\n  }\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(이메일이 이미 존재할 경우, 이름이 이미 존재할 경우 다르게 출력)")))
    })
    @PostMapping("/apple/login")
    public String appleRegisterOrLogin(@Valid @RequestBody OAuthAppleRequestDto appleLoginRequestDto, HttpServletResponse response) {
        return "애플 로그인/회원가입 성공";
    }

    @Operation(
            summary = "애플 소셜 로그인 회원탈퇴"
    )
    @DeleteMapping("/apple/me")
    public ResponseEntity<ApiResponseForm<?>> appleDeleteUser(HttpServletRequest request, HttpServletResponse response, @Valid @RequestBody(required = false) FeedbackAddDto feedbackAddDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        log.info("userId: {}", userId);
        try {
            appleLoginService.revokeToken(userId);
        } catch (Exception e) {
            log.warn("Apple 토큰 철회에 실패했지만 계정 삭제를 계속 진행합니다. userId={}, reason={}", userId, e.getMessage());
        }
        userAuthService.deleteUser(userId, feedbackAddDto);
        return ResponseEntity.ok(ApiResponseForm.success(null, "애플 로그인 회원탈퇴 성공"));
    }

    @Operation(
            summary = "구글 소셜 로그인 회원탈퇴"
    )
    @DeleteMapping("/google/me")
    public ResponseEntity<ApiResponseForm<?>> googleDeleteUser(HttpServletRequest request, HttpServletResponse response, @Valid @RequestBody(required = false) FeedbackAddDto feedbackAddDto) {
        Long userId = userAuthService.getUserIdFromToken(request);
        log.info("userId: {}", userId);
        try {
            googleLoginService.revokeToken(userId);
        } catch (Exception e) {
            log.warn("Google 토큰 철회에 실패했지만 계정 삭제를 계속 진행합니다. userId={}, reason={}", userId, e.getMessage());
        }
        userAuthService.deleteUser(userId, feedbackAddDto);
        return ResponseEntity.ok(ApiResponseForm.success(null, "구글 로그인 회원탈퇴 성공"));
    }



}
