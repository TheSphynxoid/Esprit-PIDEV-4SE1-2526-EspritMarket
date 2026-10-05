package net.thesphynx.espritmarket.EventPlanning.Config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventPlanningOpenApiConfig {
    @Bean
    public GroupedOpenApi eventplanningApi() {
        return GroupedOpenApi.builder()
                .group("eventplanning")
                .pathsToMatch("/api/eventplanning/**")
                .build();
    }
}
