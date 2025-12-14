package com.payrail.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal, resume-ready OpenAPI/Swagger configuration.
 *
 * - Documents APIs at: /v3/api-docs
 * - Swagger UI at: /swagger-ui.html
 * - Adds JWT Bearer auth button in Swagger UI
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI payrailOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayRail API")
                        .description("Stripe payment service with JWT auth, webhooks, idempotency and ledger.")
                        .version("v1"))
                // Tell Swagger UI that most endpoints require Bearer token
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
