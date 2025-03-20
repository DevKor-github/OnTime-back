package devkor.ontime_back.controller;

import devkor.ontime_back.response.ApiResponseForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Operation(
            summary = "이용약관 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "이용약관 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이용약관 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"이용약관 조회 성공\",\n  \"data\": \"이용약관 ~~~입니다\"\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "이용약관 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("/terms")
    public ResponseEntity<ApiResponseForm<?>> getTerms() {
        String terms = "이용약관 ~~~입니다.";
        String message = "이용약관 조회 성공";

        return ResponseEntity.ok(ApiResponseForm.success(terms, message));
    }

    @Operation(
            summary = "개인정보처리방침 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "개인정보처리방침 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "개인정보처리방침 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"개인정보처리방침 조회 성공\",\n  \"data\": \"개인정보처리방침 ~~~입니다.\"\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "개인정보처리방침 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("/privacy")
    public ResponseEntity<ApiResponseForm<?>> getPrivacyPolicy() {
        String terms = "개인정보처리방침 ~~~입니다.";
        String message = "개인정보처리방침 조회 성공";

        return ResponseEntity.ok(ApiResponseForm.success(terms, message));
    }


    @Operation(
            summary = "온타임 소개글 조회",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "온타임 소개글 조회 요청 JSON 데이터는 없음. 헤더에 토큰만 있으면 됨",
                    content = @Content(
                            schema = @Schema(
                                    type = "object",
                                    example = "{}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "온타임 소개글 조회 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            example = "{\n  \"status\": \"success\",\n  \"code\": \"200\",\n  \"message\": \"온타임 소개글 조회 성공\",\n  \"data\": \"온타임은 지각방지 어플리케이션으로 사용자분들이 바쁜 현대사회에서 여유를 찾을 수 있게 도와드립니다.\"\n}"
                    )
            )),
            @ApiResponse(responseCode = "4XX", description = "개인정보처리방침 조회 실패", content = @Content(mediaType = "application/json", schema = @Schema(example = "실패 메세지(정확히 어떤 메세지인지는 모름)")))
    })
    @GetMapping("/ontime-description")
    public ResponseEntity<ApiResponseForm<?>> getOntimeDescription() {
        String terms = "온타임은 지각방지 어플리케이션으로 사용자분들이 바쁜 현대사회에서 여유를 찾을 수 있게 도와드립니다.";
        String message = "온타임 소개글 조회 성공";

        return ResponseEntity.ok(ApiResponseForm.success(terms, message));
    }
}
