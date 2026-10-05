package net.thesphynx.espritmarket.Common.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class XUserAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_EMAIL = "X-User-Email";
    public static final String HEADER_ID = "X-User-Id";
    public static final String HEADER_ROLES = "X-User-Roles";
    public static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    private final String gatewayToken;

    public XUserAuthFilter(@Value("${app.gateway.token:}") String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = request.getHeader(HEADER_EMAIL);
            if (StringUtils.hasText(email) && isTrusted(request)) {
                String rolesHeader = request.getHeader(HEADER_ROLES);
                List<SimpleGrantedAuthority> authorities = Arrays.stream(
                                rolesHeader == null ? new String[0] : rolesHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UserDetails principal = org.springframework.security.core.userdetails.User.builder()
                        .username(email)
                        .password("")
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTrusted(HttpServletRequest request) {
        if (!StringUtils.hasText(gatewayToken)) {
            return true;
        }
        return gatewayToken.equals(request.getHeader(HEADER_GATEWAY_TOKEN));
    }
}
