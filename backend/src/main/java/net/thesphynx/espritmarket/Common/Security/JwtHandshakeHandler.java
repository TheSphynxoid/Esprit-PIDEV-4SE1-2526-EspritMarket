package net.thesphynx.espritmarket.Common.Security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                     Map<String, Object> attributes) {
        Object auth = attributes.get(JwtHandshakeInterceptor.AUTH_ATTR);
        if (auth instanceof Principal principal) {
            return principal;
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}
