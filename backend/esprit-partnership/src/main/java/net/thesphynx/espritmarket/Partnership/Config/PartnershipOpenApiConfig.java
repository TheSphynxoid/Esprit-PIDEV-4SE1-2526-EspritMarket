package net.thesphynx.espritmarket.Partnership.Config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PartnershipOpenApiConfig {
    @Bean
    public GroupedOpenApi partnershipApi() {
        return GroupedOpenApi.builder()
                .group("partnership")
                .pathsToMatch("/api/partnership/**")
                .build();
    }
}
