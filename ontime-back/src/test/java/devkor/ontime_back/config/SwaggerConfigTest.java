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

import java.util.List;

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
    void customizerRemovesGetBodiesAndClearsSecurityForPublicEndpoints() {
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
        assertThat(privatePost.getSecurity()).containsExactly(new SecurityRequirement().addList("accessToken"));
        assertThat(publicPost.getSecurity()).isEmpty();
    }
}
