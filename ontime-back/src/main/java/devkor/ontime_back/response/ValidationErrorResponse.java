package devkor.ontime_back.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "요청 검증 실패 상세")
public record ValidationErrorResponse(
        @Schema(description = "필드별 검증 오류 목록")
        List<FieldError> errors
) {

    @Schema(description = "필드 검증 오류")
    public record FieldError(
            @Schema(description = "오류가 발생한 필드명", example = "email")
            String field,
            @Schema(description = "검증 실패 메시지", example = "이메일 형식이 올바르지 않습니다.")
            String message
    ) {
    }
}
