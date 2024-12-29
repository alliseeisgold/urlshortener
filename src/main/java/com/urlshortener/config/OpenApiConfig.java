package com.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .version("1.0.0")
                        .description("""
                                REST API for the URL Shortener service.
                                ## Features
                                - Create short URLs with Base62 encoding
                                - Custom short codes
                                - Time-to-live (expiration)
                                - Click tracking & analytics
                                - Redis caching for fast redirects
                                """))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current server")
                ))
                .components(new Components());
    }
}
