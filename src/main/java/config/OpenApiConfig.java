package config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger configuration for ThreadVault.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI threadVaultOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ThreadVault REST API")
                        .description("High-performance Java 21 concurrent incremental backup and content deduplication engine. " +
                                "Provides asynchronous backup execution, Server-Sent Events (SSE) progress streams, " +
                                "metadata catalog inspection, and repository restoration.")
                        .version("1.0.0")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new Contact()
                                .name("ThreadVault Engineering")
                                .url("https://github.com/ayesha/ThreadVault")));
    }
}

