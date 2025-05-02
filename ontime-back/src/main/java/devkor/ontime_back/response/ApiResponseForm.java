package devkor.ontime_back.response;

import lombok.Getter;

@Getter
// @AllArgsConstructor(access = AccessLevel.PRIVATE) -> super 사용시 이용불가
// @EqualsAndHashCode(callSuper = true) // equals()와 hashCode() 메서드를 자동으로 생성하도록
public class ApiResponseForm<T> {
    // 제네릭 api 응답 객체
    private String status;
    private int code;
    private String message;
    private final T data;
    public ApiResponseForm(String status, int code, String message, T data) {
        this.status = status; // HttpResponse의 생성자 호출 (부모 클래스의 생성자 또는 메서드를 호출, 자식 클래스는 부모 클래스의 private 필드에 직접 접근 X)
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 성공 응답을 위한 메서드 (message를 받는 경우)
    public static <T> ApiResponseForm<T> success(T data, String message) {
        return new ApiResponseForm<>("success", 200, message, data);
    }

    // 성공 응답을 위한 메서드 (message를 받지 않는 경우)
    public static <T> ApiResponseForm<T> success(T data) {
        return new ApiResponseForm<>("success", 200, "OK", data);
    }

    // 실패 응답을 위한 메서드
    public static <T> ApiResponseForm<T> fail(int code, String message) {
        return new ApiResponseForm<>("fail", code, message, null);  // 실패의 경우 data는 null로 처리
    }

    public static <T> ApiResponseForm<T> accessTokenEmpty(int code, String message) {
        return new ApiResponseForm<>("accessTokenEmpty", code, message, null);  // 실패의 경우 data는 null로 처리
    }

    public static <T> ApiResponseForm<T> accessTokenInvalid(int code, String message) {
        return new ApiResponseForm<>("accessTokenInvalid", code, message, null);  // 실패의 경우 data는 null로 처리
    }

    public static <T> ApiResponseForm<T> refreshTokenInvalid(int code, String message) {
        return new ApiResponseForm<>("refreshTokenInvalid", code, message, null);  // 실패의 경우 data는 null로 처리
    }

    // 오류 응답을 위한 메서드
    public static <T> ApiResponseForm<T> error(int code, String message) {
        return new ApiResponseForm<>("error", code, message, null);  // 오류의 경우 data는 null로 처리
    }

}

