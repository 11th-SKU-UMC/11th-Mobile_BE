package com.umc.movie.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // @SecurityRequirement(name = "bearerAuth")에서 이 이름으로 참조합니다
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UMC Movie API")
                        .description("영화 평점·즐겨찾기 서비스 API 문서입니다.")
                        .version("v1"))
                // Swagger UI 오른쪽 위 Authorize 버튼에 토큰을 넣으면
                // 자물쇠가 달린 API 요청에 "Authorization: Bearer {토큰}" 헤더가 자동으로 붙습니다
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
