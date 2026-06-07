package net.thesphynx.espritmarket.Marketplace.Config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketplaceOpenApiConfig {
    @Bean
    public GroupedOpenApi marketplaceApi() {
        return GroupedOpenApi.builder()
                .group("marketplace")
                .pathsToMatch("/api/marketplace/**")
                .build();
    }
}
