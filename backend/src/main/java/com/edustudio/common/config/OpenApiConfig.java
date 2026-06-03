package com.edustudio.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eduAgentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduAgent Studio / LearnAgent-A3 后端接口")
                        .description("EduAgent Studio 与 LearnAgent-A3 为同一项目在不同文档阶段的命名过渡，本阶段以后端基础工程和数据库设计为主。")
                        .version("0.1.0")
                        .license(new License().name("Course Project")));
    }
}
