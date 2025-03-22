package devkor.ontime_back.controller;

import devkor.ontime_back.dto.*;
import devkor.ontime_back.entity.User;
import devkor.ontime_back.response.ApiResponseForm;
import devkor.ontime_back.service.FriendshipService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendShipController {

    private final UserAuthService userAuthService;
    private final FriendshipService friendShipService;


    @Operation(
            summary = "친구 추가 링크 생성 및 반환",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "친구 추가 링크 생성 및 반환 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "친구추가 링크 생성 및 반환 성공. 반환되는 UUID로 친구추가 요청 데이터를 조회하고, 친구추가 요청을 수락하거나 거절할 수 있음. (반환되는 UUID는 서버에서 랜덤 제너레이팅 된 UUID임)", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"친구추가 링크 생성 성공. 반환되는 UUID로 친구추가 요청 데이터를 조회하고, 친구추가 요청을 수락하거나 거절할 수 있음.\",\n  \"data\": {\n    \"friendShipId\": \"3fa85f64-5717-4562-b3fc-2c963f66afe5\"\n  }\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "친구추가 링크 생성 및 반환 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("/links") // 친구 추가 링크 생성
    public ResponseEntity<ApiResponseForm<CreateFriendshipLinkResponse>> createFriendShipLink(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);

        CreateFriendshipLinkResponse createFriendshipLinkResponse = CreateFriendshipLinkResponse.builder()
                .friendShipId(friendShipService.createFriendShipLink(userId))
                .build();

        String message = "친구추가 링크 생성 및 반환 성공. 반환되는 UUID로 친구추가 요청 데이터를 조회하고, 친구추가 요청을 수락하거나 거절할 수 있음.";
        return ResponseEntity.ok(ApiResponseForm.success(createFriendshipLinkResponse, message));
    }

    @Operation(
            summary = "친구 추가 요청자 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "친구 추가 요청자 조회 JSON 데이터는 없음. 헤더에 토큰과 쿼리파라미터(UUID)만 있으면 됨",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "친구추가 요청자 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"친구추가 요청자 조회 성공\",\n  \"data\": {\n    \"requesterId\": 2,\n    \"requesterName\": \"junbeommmm\",\n    \"requesterEmail\": \"userrrr@example.com\"\n  }\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "친구추가 요청자 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("/{uuid}/requests") // 친구 추가 요청자 조회
    public ResponseEntity<ApiResponseForm<GetFriendshipRequesterResponse>> getFriendShipRequester(HttpServletRequest request, @PathVariable String uuid) {
        Long userId = userAuthService.getUserIdFromToken(request);

        User requester = friendShipService.getFriendShipRequester(userId, UUID.fromString(uuid));
        GetFriendshipRequesterResponse getFriendshipRequesterResponse = GetFriendshipRequesterResponse.builder()
                .requesterId(requester.getId())
                .requesterName(requester.getName())
                .requesterEmail(requester.getEmail())
                .build();

        String message = "친구추가 요청자 조회 성공";
        return ResponseEntity.ok(ApiResponseForm.success(getFriendshipRequesterResponse, message));
    }

    @Operation(
            summary = "친구추가 수락상태 업데이트",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "친구추가 수락상태 업데이트 요청 JSON 데이터. acceptStatus 값으로 \"ACCEPTED\" 또는 \"REJECTED\"만 가능함.",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{\n  \"acceptStatus\": \"ACCEPTED\"\n}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "친구추가 요청을 수락 또는 거절할 수 있음. 거절 시에는 \"친구추가 요청 거절 성공\" 메세지가 출력됨.", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"친구추가 요청 수락 성공\",\n  \"data\": null\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "친구추가 수락상태 업데이트 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @PostMapping("/{uuid}/approve") // 친구 추가 요청 수락
    public ResponseEntity<ApiResponseForm<String>> updateAcceptStatus(HttpServletRequest request, @PathVariable String uuid, @RequestBody UpdateAcceptStatusDto updateAcceptStatusDto) {
        Long userId = userAuthService.getUserIdFromToken(request);

        friendShipService.updateAcceptStatus(userId, UUID.fromString(uuid), updateAcceptStatusDto.getAcceptStatus());

        String status = updateAcceptStatusDto.getAcceptStatus().equals("ACCEPTED") ? "수락" : "거절";
        String message = "친구추가 요청 " + status + " 성공";
        return ResponseEntity.ok(ApiResponseForm.success(null, message));
    }

    @Operation(
            summary = "친구 목록 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "친구 목록 조회 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "친구 목록 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"친구 목록 조회 성공\",\n  \"data\": [\n    {\n      \"friendId\": 2,\n      \"friendName\": \"junbeommmm\",\n      \"friendEmail\": \"userrrr@example.com\"\n    },\n    {\n      \"friendId\": 3,\n      \"friendName\": \"jinseoooo\",\n      \"friendEmail\": \"usererer@example.com\"\n    }\n  ]\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "친구 목록 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("") // 친구 목록 조회
    public ResponseEntity<ApiResponseForm<GetFriendListResponse>> getFriendList(HttpServletRequest request) {
        Long userId = userAuthService.getUserIdFromToken(request);

        List<FriendDto> friendsList = friendShipService.getFriendList(userId);
        GetFriendListResponse getFriendsListResponse = GetFriendListResponse.builder()
                .friendsList(friendsList)
                .build();

        String message = "친구 목록 조회 성공";
        return ResponseEntity.ok(ApiResponseForm.success(getFriendsListResponse, message));
    }

}
