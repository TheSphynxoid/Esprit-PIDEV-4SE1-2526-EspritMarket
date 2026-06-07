package net.thesphynx.espritmarket.Delivery.Config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryOpenApiConfig {
    @Bean
    public GroupedOpenApi deliveryApi() {
        return GroupedOpenApi.builder()
                .group("delivery")
                .pathsToMatch("/api/delivery/**")
                .build();
    }
}
