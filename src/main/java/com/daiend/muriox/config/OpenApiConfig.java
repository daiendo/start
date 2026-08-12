package com.daiend.muriox.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .scheme("bearer").bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("Muriox API")
                        .version("1.0")
                        .description("Muriox 后端接口文档"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, securityScheme))
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_AUTH));
    }
}
