package org.aitu.vulnerabilitiesmvp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fintechOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Secure Payments MVP API")
            .description("JWT-protected fintech MVP with fraud monitoring")
            .version("1.0.0")
            .contact(new Contact().name("AITU Project")));
    }
}
