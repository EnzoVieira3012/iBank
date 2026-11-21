package com.ibank.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("iBank API")
                .version("1.0.0")
                .description("""
                    ## iBank - Banking API
                    
                    Complete banking API with:
                    * **Accounts** - Current and Savings accounts
                    * **PIX** - Keys, Payments, QR Codes, Refunds
                    * **Transfers** - TED, Internal, Scheduled
                    * **Credit** - Loans, Financing (Price/SAC)
                    * **Cards** - Credit/Debit, Invoices, Limits
                    * **Fees** - Tariffs, Interest rates
                    * **Limits** - Daily/Monthly limits, Blocks
                    * **Antifraud** - Rules, Scoring, Alerts
                    * **Audit** - Immutable logs, Traceability
                    * **Reconciliation** - CNAB, Matching, Reports
                    
                    ### Authentication
                    All endpoints require JWT Bearer token.
                    Include `Authorization: Bearer <token>` header.
                    
                    ### Idempotency
                    All mutating endpoints (POST/PUT/PATCH) require `Idempotency-Key` header (UUID v4).
                    
                    ### Correlation ID
                    Include `X-Correlation-ID` header for request tracing.
                    
                    ### Rate Limiting
                    100 requests/minute per IP. Exceeding returns 429 with `Retry-After`.
                    """)
                .contact(new Contact()
                    .name("iBank Team")
                    .email("dev@ibank.local")
                    .url("https://github.com/EnzoVieira3012/iBank"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url("http://localhost:" + serverPort).description("Local Development"),
                new Server().url("https://api.ibank.local").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token obtained from /api/v1/auth/login"))
                .addSecuritySchemes("idempotencyKey", new SecurityScheme()
                    .name("Idempotency-Key")
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .description("UUID v4 for idempotent requests"))
                .addSecuritySchemes("correlationId", new SecurityScheme()
                    .name("X-Correlation-ID")
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .description("Correlation ID for request tracing"))
            )
            .tags(List.of(
                new Tag().name("Auth").description("Autenticação e autorização"),
                new Tag().name("Accounts").description("Contas correntes e poupança"),
                new Tag().name("PIX").description("PIX - Chaves, Pagamentos, QR Codes, Devoluções"),
                new Tag().name("Transfers").description("TED, DOC, Transferências Internas, Agendadas"),
                new Tag().name("Credit").description("Empréstimos e Financiamentos (Price/SAC)"),
                new Tag().name("Cards").description("Cartões Crédito/Débito, Faturas, Limites"),
                new Tag().name("Fees").description("Tarifas e Juros"),
                new Tag().name("Limits").description("Limites e Bloqueios"),
                new Tag().name("Antifraud").description("Regras, Alertas, Scoring"),
                new Tag().name("Audit").description("Auditoria e Rastreabilidade"),
                new Tag().name("Reconciliation").description("Conciliação Financeira")
            ))
            .externalDocs(new ExternalDocumentation()
                .description("iBank Wiki")
                .url("https://github.com/EnzoVieira3012/iBank/wiki"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/api/v1/**")
            .packagesToScan("com.ibank.api.controller")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/v1/admin/**")
            .packagesToScan("com.ibank.api.controller.admin")
            .build();
    }
}