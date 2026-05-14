package devkor.ontime_back.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://3.38.172.54.nip.io", description = "New Production Server"),
                @Server(url = "http://localhost:8080", description = "Local Server")
        }
)
public class SwaggerConfig {
    private static final String ACCESS_TOKEN_SCHEME = "accessToken";
    private static final String REFRESH_TOKEN_SCHEME = "refreshToken";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/",
            "/health",
            "/account-deletion",
            "/privacy-policy",
            "/sign-up",
            "/login",
            "/oauth2/google/login",
            "/oauth2/kakao/login",
            "/oauth2/apple/login",
            "/swagger-ui.html",
            "/error"
    );

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/css",
            "/images",
            "/js",
            "/favicon.ico",
            "/h2-console"
    );

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(ACCESS_TOKEN_SCHEME, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Send as `Authorization: Bearer <access token>`.")
                        )
                        .addSecuritySchemes(REFRESH_TOKEN_SCHEME, new SecurityScheme()
                                .name("Authorization-refresh")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Send as `Authorization-refresh: Bearer <refresh token>` to reissue an access token.")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(ACCESS_TOKEN_SCHEME))
                .info(apiInfo());
    }

    @Bean
    public OpenApiCustomizer coherentOperationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (pathItem == null) {
                    return;
                }

                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if (httpMethod == PathItem.HttpMethod.GET) {
                        operation.setRequestBody(null);
                    }
                    if (isPublicPath(path)) {
                        operation.setSecurity(Collections.emptyList());
                    }
                    removeStaleResponseExamples(operation);
                });
            });
        };
    }

    private Info apiInfo() {
        return new Info()
                .title("Ontime")
                .description("""
                        Ontime API 명세서

                        [JWT 인증 과정]
                        공개 엔드포인트(`/sign-up`, `/login`, `/oauth2/google/login`, `/oauth2/kakao/login`, `/oauth2/apple/login`, `/health`, `/account-deletion`, `/privacy-policy`)를 제외한 API는 access token이 필요합니다.

                        Access token 요청 형식: `Authorization: Bearer <access token>`

                        Refresh token으로 access token을 재발급할 때는 보호 API 호출 전에 `Authorization-refresh: Bearer <refresh token>` 헤더를 보냅니다. 재발급 성공 시 새 access token은 응답 헤더 `Authorization`으로 반환됩니다.

                        일반 로그인과 소셜 로그인, 회원가입 성공 시 access token은 `Authorization` 헤더로, refresh token은 `Authorization-refresh` 헤더로 반환됩니다.
                        """)
                .version("1.0.0");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path)
                || PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void removeStaleResponseExamples(Operation operation) {
        if (operation.getResponses() == null) {
            return;
        }

        operation.getResponses().values().forEach(apiResponse -> {
            if (apiResponse.getContent() == null) {
                return;
            }

            apiResponse.getContent().values().forEach(mediaType -> {
                mediaType.setExample(null);
                mediaType.setExamples(null);
                if (mediaType.getSchema() != null) {
                    mediaType.getSchema().setExample(null);
                }
            });
        });
    }
}
