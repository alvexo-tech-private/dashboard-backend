package com.alvexo.adminportal.config;

import com.alvexo.adminportal.entity.AdminRole;
import com.alvexo.adminportal.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the Authorization: Bearer header on every request, validates it via
 * JwtService, and (if valid) sets an AdminPrincipal + ROLE_<role> authority
 * on the SecurityContext. Never rejects a request itself — an absent/invalid
 * token just means the context stays unauthenticated, and Spring Security's
 * own authorizeHttpRequests rules decide whether that's allowed.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Claims claims = jwtService.parseClaims(token);
            if (claims != null) {
                AdminPrincipal principal = new AdminPrincipal(
                        Long.valueOf(claims.getSubject()),
                        claims.get("email", String.class),
                        claims.get("fullName", String.class),
                        AdminRole.valueOf(claims.get("role", String.class)));
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
