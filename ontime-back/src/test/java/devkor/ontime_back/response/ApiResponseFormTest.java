package devkor.ontime_back.response;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseFormTest {

    @Test
    void successEnvelopeUsesOkDefaultsWhenMessageIsOmitted() {
        ApiResponseForm<Map<String, String>> response = ApiResponseForm.success(Map.of("id", "1"));

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getData()).containsEntry("id", "1");
    }

    @Test
    void authErrorFactoriesExposeDistinctTokenStatuses() {
        assertThat(ApiResponseForm.accessTokenEmpty(401, "missing").getStatus()).isEqualTo("accessTokenEmpty");
        assertThat(ApiResponseForm.accessTokenInvalid(401, "bad").getStatus()).isEqualTo("accessTokenInvalid");
        assertThat(ApiResponseForm.refreshTokenInvalid(401, "bad refresh").getStatus()).isEqualTo("refreshTokenInvalid");
    }

    @Test
    void failAndErrorEnvelopesDoNotIncludeSuccessDataByDefault() {
        ApiResponseForm<Void> fail = ApiResponseForm.fail(400, "bad request");
        ApiResponseForm<Void> numericError = ApiResponseForm.error(500, "server error");
        ApiResponseForm<Void> symbolicError = ApiResponseForm.error("SCHEDULE_ALREADY_STARTED", "already started");

        assertThat(fail.getStatus()).isEqualTo("fail");
        assertThat(fail.getData()).isNull();
        assertThat(numericError.getStatus()).isEqualTo("error");
        assertThat(numericError.getCode()).isEqualTo(500);
        assertThat(symbolicError.getCode()).isEqualTo("SCHEDULE_ALREADY_STARTED");
    }

    @Test
    void errorEnvelopeCanCarryValidationDetails() {
        ApiResponseForm<Map<String, String>> response = ApiResponseForm.error(
                400,
                "validation failed",
                Map.of("field", "email")
        );

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getData()).containsEntry("field", "email");
    }
}
