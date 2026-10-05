    package net.thesphynx.espritmarket.Common.Config;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.authorization.AuthorizationManager;
    import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
    import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

    import org.springframework.messaging.Message;

    @Configuration
    @EnableWebSocketSecurity
    public class WebSocketSecurityConfig {

        @Bean
        AuthorizationManager<Message<?>> messageAuthorizationManager(
                MessageMatcherDelegatingAuthorizationManager.Builder messages) {

            messages
                    .simpDestMatchers("/app/**").authenticated()
                    .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").authenticated()
                    .anyMessage().authenticated();

            return messages.build();
        }
    }
