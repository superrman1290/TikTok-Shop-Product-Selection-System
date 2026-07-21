package com.tiktokinsight.common.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI productInsightOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TikTok Product Insight API")
                .description("HTTP API for the TikTok Shop product selection system")
                .version("v1"));
    }
}
