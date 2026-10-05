package net.thesphynx.espritmarket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import net.thesphynx.espritmarket.Common.Security.JwtService;
import net.thesphynx.espritmarket.Common.Security.XUserAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationFilter.class);
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final Set<String> PUBLIC_GET = Set.of(
            "/api/market/products",
            "/api/visual-search",
            "/api/marketplace/semantic-search",
            "/api/delivery/maps/config",
            "/api/srv/services/**",
            "/api/srv/service-reviews/**",
            "/api/srv/services/images/**",
            "/api/eventplanning/events",
            "/api/eventplanning/events/with-participants",
            "/api/eventplanning/events/*/with-participants",
            "/api/eventplanning/tickets/promo-dates",
            "/api/eventplanning/tickets/promo-offers",
            "/api/eventplanning/tickets/promo-selection",
            "/uploads/**"
    );

    private static final Set<String> PUBLIC_POST = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/visual-search",
            "/api/marketplace/semantic-search"
    );

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/actuator",
            "/ws",
            "/ws-marketplace",
            "/ws-marketplace-native",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private final JwtService jwtService;
    private final String gatewayToken;

    public JwtValidationFilter(JwtService jwtService,
                               @Value("${app.gateway.token:}") String gatewayToken) {
        this.jwtService = jwtService;
        this.gatewayToken = gatewayToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        if (isPublic(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            reject(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        String email;
        try {
            email = jwtService.extractEmail(token);
            if (!StringUtils.hasText(email)) {
                reject(response, HttpStatus.UNAUTHORIZED, "Invalid token");
                return;
            }
            if (!jwtService.isAccessToken(token)) {
                reject(response, HttpStatus.UNAUTHORIZED, "Access token required");
                return;
            }
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            reject(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        Object userId = null;
        try {
            userId = jwtService.extractClaim(token, claims -> claims.get("userId") != null
                    ? claims.get("userId")
                    : (claims.get("user_id") != null ? claims.get("user_id") : claims.get("id")));
        } catch (Exception ignored) {
        }

        List<String> roles = List.of();
        try {
            List<?> rawRoles = jwtService.extractClaim(token, claims -> (List<?>) claims.get("roles"));
            if (rawRoles != null) {
                roles = rawRoles.stream().map(String::valueOf).toList();
            }
        } catch (Exception ignored) {
        }

        Map<String, String> added = new HashMap<>();
        added.put(XUserAuthFilter.HEADER_EMAIL, email);
        if (userId != null) {
            added.put(XUserAuthFilter.HEADER_ID, String.valueOf(userId));
        }
        added.put(XUserAuthFilter.HEADER_ROLES, String.join(",", roles));
        if (StringUtils.hasText(gatewayToken)) {
            added.put(XUserAuthFilter.HEADER_GATEWAY_TOKEN, gatewayToken);
        }

        filterChain.doFilter(new HeaderMutatingRequest(request, added), response);
    }

    private boolean isPublic(String path, HttpMethod method) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.equals(prefix)) {
                return true;
            }
        }
        if (method.equals(HttpMethod.GET)) {
            for (String pattern : PUBLIC_GET) {
                if (MATCHER.match(pattern, path)) {
                    return true;
                }
            }
        }
        if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.OPTIONS)) {
            if (method.equals(HttpMethod.OPTIONS)) {
                return true;
            }
            for (String pattern : PUBLIC_POST) {
                if (MATCHER.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message + "\"}");
    }

    private static class HeaderMutatingRequest extends HttpServletRequestWrapper {

        private final Map<String, String> added;

        HeaderMutatingRequest(HttpServletRequest request, Map<String, String> added) {
            super(request);
            this.added = added;
        }

        private boolean isRemoved(String name) {
            String lower = name.toLowerCase();
            return lower.equals("authorization")
                    || lower.equals(XUserAuthFilter.HEADER_EMAIL.toLowerCase())
                    || lower.equals(XUserAuthFilter.HEADER_ID.toLowerCase())
                    || lower.equals(XUserAuthFilter.HEADER_ROLES.toLowerCase())
                    || lower.equals(XUserAuthFilter.HEADER_GATEWAY_TOKEN.toLowerCase());
        }

        @Override
        public String getHeader(String name) {
            for (Map.Entry<String, String> entry : added.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            if (isRemoved(name)) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            for (Map.Entry<String, String> entry : added.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(entry.getValue()));
                }
            }
            if (isRemoved(name)) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String name = original.nextElement();
                if (!isRemoved(name)) {
                    names.add(name);
                }
            }
            names.addAll(added.keySet());
            return Collections.enumeration(names);
        }
    }
}
