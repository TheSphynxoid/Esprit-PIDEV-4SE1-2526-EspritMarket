package net.thesphynx.espritmarket.Srv.Config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SrvOpenApiConfig {
    @Bean
    public GroupedOpenApi srvApi() {
        return GroupedOpenApi.builder()
                .group("srv")
                .pathsToMatch("/api/srv/**")
                .build();
    }
}
