package com.alvexo.adminportal.config;

import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AdminUserStatus;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the Authorization: Bearer header on every request, validates it via
 * JwtService, then re-reads the admin's row so a Super Admin's changes apply
 * straight away rather than when the token expires (ADMIN-US-29 BR-13, BR-16):
 * a disabled or missing admin stays unauthenticated (401), and the role comes
 * from the database, not the token.
 *
 * An admin still on a temporary password may only read their profile and
 * change their password; anything else gets 403 PASSWORD_CHANGE_REQUIRED.
 *
 * Never rejects a request for a missing/invalid token itself: the context
 * just stays unauthenticated and SecurityConfig's rules decide.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final Set<String> PASSWORD_CHANGE_PATHS =
            Set.of("/api/auth/login", "/api/auth/me", "/api/auth/change-password");

    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<AdminUser> admin = activeAdmin(request.getHeader("Authorization"));
        if (admin.isPresent()) {
            AdminUser a = admin.get();
            if (a.isMustChangePassword() && !PASSWORD_CHANGE_PATHS.contains(path(request))) {
                writePasswordChangeRequired(response);
                return;
            }
            AdminPrincipal principal = new AdminPrincipal(a.getId(), a.getEmail(), a.getFullName(), a.getRole());
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + a.getRole().name()));
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        }
        filterChain.doFilter(request, response);
    }

    private Optional<AdminUser> activeAdmin(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        Claims claims = jwtService.parseClaims(header.substring(7));
        if (claims == null) {
            return Optional.empty();
        }
        long id;
        try {
            id = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return adminUserRepository.findById(id).filter(a -> a.getStatus() == AdminUserStatus.ACTIVE);
    }

    /** Request path without the servlet context path (/adminapi). */
    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static void writePasswordChangeRequired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"Please choose a new password before continuing.\","
                + "\"data\":null,\"errorCode\":\"PASSWORD_CHANGE_REQUIRED\"}");
    }
}
