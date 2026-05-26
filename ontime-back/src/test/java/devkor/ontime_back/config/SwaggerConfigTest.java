package devkor.ontime_back.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Test
    void openApiDocumentsRealTokenHeaders() {
        OpenAPI openAPI = swaggerConfig.openAPI();

        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKeys("accessToken", "refreshToken");
        assertThat(openAPI.getSecurity())
                .containsExactly(new SecurityRequirement().addList("accessToken"));
        assertThat(openAPI.getInfo().getDescription())
                .contains("Authorization: Bearer <access token>")
                .contains("Authorization-refresh: Bearer <refresh token>")
                .contains("응답 헤더 `Authorization`");
    }

    @Test
    void customizerAddsOperationExamplesAndKeepsCoherenceRules() {
        MediaType staleResponseContent = new MediaType()
                .schema(new StringSchema().example("stale"))
                .example("stale");
        Operation privateGet = new Operation()
                .requestBody(new RequestBody())
                .security(List.of(new SecurityRequirement().addList("accessToken")))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .content(new Content().addMediaType("application/json", staleResponseContent))));
        Operation publicPost = new Operation()
                .security(List.of(new SecurityRequirement().addList("accessToken")));
        Operation privatePost = new Operation()
                .security(List.of(new SecurityRequirement().addList("accessToken")));

        OpenAPI openAPI = new OpenAPI().paths(new Paths()
                .addPathItem("/schedules", new PathItem()
                        .get(privateGet)
                        .post(privatePost))
                .addPathItem("/login", new PathItem()
                        .post(publicPost)));

        swaggerConfig.coherentOperationCustomizer().customise(openAPI);

        assertThat(privateGet.getRequestBody()).isNull();
        assertThat(privateGet.getSecurity()).containsExactly(new SecurityRequirement().addList("accessToken"));
        assertThat(staleResponseContent.getExample()).isNull();
        assertThat(staleResponseContent.getSchema().getExample()).isNull();
        assertThat(responseExamples(privateGet, "200")).containsKey("Success");
        assertThat(responseExamples(privateGet, "400")).containsKeys("Validation error", "Malformed JSON");
        assertThat(responseExamples(privateGet, "401")).containsKeys("Missing access token", "Invalid access token", "Invalid refresh token");
        assertThat(responseExamples(privateGet, "500")).containsKey("Unexpected error");
        assertThat(privatePost.getSecurity()).containsExactly(new SecurityRequirement().addList("accessToken"));
        assertThat(responseExamples(privatePost, "200")).containsKey("Success");
        assertThat(publicPost.getSecurity()).isEmpty();
        assertThat(responseExamples(publicPost, "200")).containsKey("Success");
        assertThat(responseExamples(publicPost, "401")).isEmpty();
    }

    @Test
    void customizerAddsExamplesForEveryControllerEndpointCase() {
        OpenAPI openAPI = new OpenAPI().paths(new Paths()
                .addPathItem("/health", new PathItem().get(operation()))
                .addPathItem("/account-deletion", new PathItem().get(operation()))
                .addPathItem("/account-deletion/en", new PathItem().get(operation()))
                .addPathItem("/privacy-policy", new PathItem().get(operation()))
                .addPathItem("/privacy-policy/en", new PathItem().get(operation()))
                .addPathItem("/users/me/alarm-settings", new PathItem().get(operation()).patch(operationWithBody()))
                .addPathItem("/users/me/analytics-preference", new PathItem().get(operation()).put(operationWithBody()))
                .addPathItem("/users/me/devices/current", new PathItem().put(operationWithBody()).delete(operationWithBody()))
                .addPathItem("/users/me/alarm-status", new PathItem().post(operationWithBody()).get(operation()))
                .addPathItem("/documents/terms", new PathItem().get(operation()))
                .addPathItem("/documents/privacy", new PathItem().get(operation()))
                .addPathItem("/documents/ontime-description", new PathItem().get(operation()))
                .addPathItem("/feedback", new PathItem().post(operationWithBody()))
                .addPathItem("/firebase-token", new PathItem().post(operationWithBody()))
                .addPathItem("/firebase-token/push-test", new PathItem().post(operation()))
                .addPathItem("/friends/links", new PathItem().post(operation()))
                .addPathItem("/friends/{uuid}/requests", new PathItem().get(operation()))
                .addPathItem("/friends/{uuid}/approve", new PathItem().post(operationWithBody()))
                .addPathItem("/friends", new PathItem().get(operation()))
                .addPathItem("/schedules", new PathItem().get(operation()).post(operationWithBody()))
                .addPathItem("/schedules/alarm-window", new PathItem().get(operation()))
                .addPathItem("/schedules/{scheduleId}", new PathItem().get(operation()).put(operationWithBody()).delete(operation()))
                .addPathItem("/schedules/{scheduleId}/start", new PathItem().post(operation()))
                .addPathItem("/schedules/lateness-history", new PathItem().get(operation()))
                .addPathItem("/schedules/{scheduleId}/preparations", new PathItem().get(operation()).post(operationWithBody()).put(operationWithBody()))
                .addPathItem("/schedules/{scheduleId}/finish", new PathItem().put(operationWithBody()))
                .addPathItem("/oauth2/google/login", new PathItem().post(operationWithBody()))
                .addPathItem("/oauth2/kakao/login", new PathItem().post(operationWithBody()))
                .addPathItem("/oauth2/apple/login", new PathItem().post(operationWithBody()))
                .addPathItem("/oauth2/apple/me", new PathItem().delete(operationWithBody()))
                .addPathItem("/oauth2/google/me", new PathItem().delete(operationWithBody()))
                .addPathItem("/sign-up", new PathItem().post(operationWithBody()))
                .addPathItem("/login", new PathItem().post(operationWithBody()))
                .addPathItem("/users/me/password", new PathItem().put(operationWithBody()))
                .addPathItem("/users/me/delete", new PathItem().delete(operationWithBody()))
                .addPathItem("/users/me/punctuality-score", new PathItem().get(operation()).put(operation()))
                .addPathItem("/users/me/spare-time", new PathItem().put(operationWithBody()))
                .addPathItem("/users/me/onboarding", new PathItem().put(operationWithBody()))
                .addPathItem("/users/me", new PathItem().get(operation()))
                .addPathItem("/users/preparations", new PathItem().put(operationWithBody()).get(operation()))
                .addPathItem("/users/me/settings", new PathItem().put(operationWithBody()))
                .addPathItem("/users/me/settings/reset", new PathItem().put(operation())));

        swaggerConfig.coherentOperationCustomizer().customise(openAPI);

        openAPI.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    assertThat(anyResponseExamples(operation, "200"))
                            .as("%s %s success examples", method, path)
                            .isNotEmpty();

                    if (!path.startsWith("/account-deletion") && !path.startsWith("/privacy-policy") && !"/health".equals(path)) {
                        assertThat(responseExamples(operation, "400"))
                                .as("%s %s bad request examples", method, path)
                                .isNotEmpty();
                        assertThat(responseExamples(operation, "500"))
                                .as("%s %s unexpected error examples", method, path)
                                .isNotEmpty();
                    }

                    if (operation.getRequestBody() != null) {
                        assertThat(requestExamples(operation))
                                .as("%s %s request examples", method, path)
                                .isNotEmpty();
                    }
                }));
    }

    private Operation operation() {
        return new Operation()
                .summary("test operation")
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("OK")));
    }

    private Operation operationWithBody() {
        return operation().requestBody(new RequestBody());
    }

    private Map<String, ?> requestExamples(Operation operation) {
        return Optional.ofNullable(operation.getRequestBody())
                .map(RequestBody::getContent)
                .map(content -> content.get("application/json"))
                .map(MediaType::getExamples)
                .orElse(Map.of());
    }

    private Map<String, ?> responseExamples(Operation operation, String code) {
        return Optional.ofNullable(operation.getResponses())
                .map(responses -> responses.get(code))
                .map(ApiResponse::getContent)
                .map(content -> content.get("application/json"))
                .map(MediaType::getExamples)
                .orElse(Map.of());
    }

    private Map<String, ?> anyResponseExamples(Operation operation, String code) {
        return Optional.ofNullable(operation.getResponses())
                .map(responses -> responses.get(code))
                .map(ApiResponse::getContent)
                .flatMap(content -> content.values().stream()
                        .map(MediaType::getExamples)
                        .filter(examples -> examples != null && !examples.isEmpty())
                        .findFirst())
                .orElse(Map.of());
    }
}
