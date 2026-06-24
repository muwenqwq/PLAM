package com.edustudio.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eduAgentOpenApi() {
        String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(
                        securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .info(new Info()
                        .title("EduAgent Studio / LearnAgent-A3 后端接口")
                        .description("EduAgent Studio 与 LearnAgent-A3 为同一项目在不同文档阶段的命名过渡。当前接口文档已展示 JWT Bearer 认证方式。")
                        .version("0.1.0")
                        .license(new License().name("Course Project")));
    }
}
