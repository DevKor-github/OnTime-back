package devkor.ontime_back.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://ontime.devkor.club", description = "Production Server")
        }
)
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("accessToken", new SecurityScheme()
                                .name("Authorization") // 헤더 이름
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("accessToken")) // 요청에 SecurityScheme 적용
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Ontime")
                .description("Ontime API 명세서\n\n\n\n [JWT 인증 과정]\n\n/sign-up, /login, /{userId}/additional-info\n\n위 세 url을 제외하고는 헤더에 엑세스 토큰을 담아 요청을 보내야 함.\n\n(형식: \"Authorization [엑세스 토큰]\")\n\n\n토큰이 유효하면 요청이 처리될 것이고, 토큰이 유효하지 않으면 실패메세지가 반환될 것임.\n\n\n 엑세스토큰 인증이 실패하면 동일한 url(사실 아무 url이나 상관 없음. 실제로 해당 url로 요청 보내기전에 필터가 가로채서 처리함)로 헤더에 리프레시토큰을 담아 요청을 보내면 리프레시토큰의 유효성에 따라 엑세스토큰이 ResponseBody 재발급 될 것임.\n\n(형식: \"Authorization-refresh [리프레시토큰]\")")
                .version("1.0.0");
    }
}
