package devkor.ontime_back.controller;

import devkor.ontime_back.dto.OAuthAppleRequestDto;
import devkor.ontime_back.dto.OAuthGoogleUserDto;
import devkor.ontime_back.dto.OAuthKakaoUserDto;
import devkor.ontime_back.global.oauth.apple.AppleLoginService;
import devkor.ontime_back.global.oauth.google.GoogleLoginService;
import devkor.ontime_back.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                    description = "구글 회원정보 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\n \"accessToken\": \"ya29.xxxxxxx\" }"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "구글 로그인/회원가입 성공 (로그인시 data : login, 회원가입시 data : register", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"message\": \"유저의 ROLE이 GUEST이므로 온보딩API를 호출해 온보딩을 진행해야합니다.\",\n  \"role\": \"GUEST\"}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(이메일이 이미 존재할 경우, 이름이 이미 존재할 경우 다르게 출력)")))
    })
    @PostMapping("/google/registerOrLogin")
    public String googleRegisterOrLogin(@RequestBody OAuthGoogleUserDto oAuthGoogleUserDto, HttpServletResponse response) {
        return "구글 로그인/회원가입 성공"; // 로그인 처리는 필터에서 적용
    }

    @Operation(
            summary = "카카오 소셜 로그인/회원가입",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "카카오 회원정보 데이터",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\n \"id\": \"4803687123\", \n \"profile\": {\n \"nickname\": \"김철수\", \n \"thumbnail_image_url\": \"http://dfsklafj;ewoai.jpg\", \n \"profile_image_url\": \"http://dfsklafj;ewoai.jpg\", \n\"is_default_image\": false, \n \"is_default_nickname\": false\n }\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카카오 로그인/회원가입 성공 (로그인시 data : login, 회원가입시 data : register", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"message\": \"유저의 ROLE이 GUEST이므로 온보딩API를 호출해 온보딩을 진행해야합니다.\",\n  \"role\": \"GUEST\"}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(이메일이 이미 존재할 경우, 이름이 이미 존재할 경우 다르게 출력)")))
    })
    @PostMapping("/kakao/registerOrLogin")
    public String kakaoRegisterOrLogin(@RequestBody OAuthKakaoUserDto oAuthKakaoUserDto, HttpServletResponse response) {
        return "카카오 로그인/회원가입 성공"; // 로그인 처리는 필터에서 적용
    }

    @Operation(
            summary = "애플 소셜 로그인/회원가입",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "애플 idtoken, authcode, fullname",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\n \"idToken\": \".\",\n  \"authCode\": \".\",\n  \"fullName\": \"허진서\" }"                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "카카오 로그인/회원가입 성공 (로그인시 data : login, 회원가입시 data : register", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            type = "object",
                            example = "{\n \"status\": \"success\",\n \"code\": \"200\",\n \"message\": \"%s\",\n \"data\": { \"userId\": %d,\n \"email\": \"%s\",\n \"name\": \"%s\",\n \"spareTime\": \"%s\",\n \"note\": \"%s\",\n \"punctualityScore\": %f,\n \"role\": \"%s\" }\n }"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(이메일이 이미 존재할 경우, 이름이 이미 존재할 경우 다르게 출력)")))
    })
    @PostMapping("/apple/registerOrLogin")
    public String appleRegisterOrLogin(@RequestBody OAuthAppleRequestDto appleLoginRequestDto, HttpServletResponse response) {
        return "애플 로그인/회원가입 성공";
    }

    @Operation(
            summary = "애플 소셜 로그인 회원탈퇴"
    )
    @PostMapping("/apple/deleteUser")
    public String appleDeleteUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = userAuthService.getUserIdFromToken(request);
        log.info("userId: {}", userId);
        appleLoginService.revokeToken(userId);
        userAuthService.deleteUser(userId);
        return "애플 로그인 회원탈퇴 성공";
    }

    @Operation(
            summary = "구글 소셜 로그인 회원탈퇴"
    )
    @PostMapping("/google/deleteUser")
    public String googleDeleteUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = userAuthService.getUserIdFromToken(request);
        log.info("userId: {}", userId);
        googleLoginService.revokeToken(userId);
        userAuthService.deleteUser(userId);
        return "애플 로그인 회원탈퇴 성공";
    }



}
