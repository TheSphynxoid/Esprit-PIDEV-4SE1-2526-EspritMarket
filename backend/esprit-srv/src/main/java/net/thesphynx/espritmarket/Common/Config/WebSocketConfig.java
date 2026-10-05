package net.thesphynx.espritmarket.Common.Config;

import net.thesphynx.espritmarket.Common.Security.JwtHandshakeHandler;
import net.thesphynx.espritmarket.Common.Security.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final JwtHandshakeHandler jwtHandshakeHandler;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                          JwtHandshakeHandler jwtHandshakeHandler,
                          @Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.jwtHandshakeHandler = jwtHandshakeHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        registry.addEndpoint("/ws-marketplace")
                .setAllowedOriginPatterns(origins.toArray(new String[0]))
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins.toArray(new String[0]))
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(jwtHandshakeHandler)
                .withSockJS();

        registry.addEndpoint("/ws-marketplace-native")
                .setAllowedOriginPatterns(origins.toArray(new String[0]));
    }
}
