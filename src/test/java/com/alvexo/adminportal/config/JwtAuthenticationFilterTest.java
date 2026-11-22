package com.alvexo.adminportal.config;

import com.alvexo.adminportal.entity.AdminRole;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AdminUserStatus;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Uses a real JwtService (test-only key) and a mocked repository; no database. */
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtService jwtService;
    private AdminUserRepository repository;
    private JwtAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, 60_000);
        repository = mock(AdminUserRepository.class);
        filter = new JwtAuthenticationFilter(jwtService, repository);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private AdminUser admin(AdminRole role, AdminUserStatus status, boolean mustChange) {
        AdminUser admin = AdminUser.builder().id(5L).email("a@alvexo.com").fullName("A")
                .role(role).status(status).mustChangePassword(mustChange).build();
        when(repository.findById(5L)).thenReturn(Optional.of(admin));
        return admin;
    }

    private MockHttpServletRequest request(String path, AdminUser tokenFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/adminapi" + path);
        request.setContextPath("/adminapi");
        request.addHeader("Authorization", "Bearer " + jwtService.generateAccessToken(tokenFor));
        return request;
    }

    @Test
    void usesTheRoleFromTheDatabaseNotTheToken() throws Exception {
        AdminUser admin = admin(AdminRole.SUPER_ADMIN, AdminUserStatus.ACTIVE, false);
        MockHttpServletRequest request = request("/api/admin-users", admin);
        admin.setRole(AdminRole.FINANCE_ADMIN); // changed after the token was issued

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(AdminRole.FINANCE_ADMIN, ((AdminPrincipal) auth.getPrincipal()).role());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FINANCE_ADMIN")));
        verify(chain).doFilter(any(), any());
    }

    @Test
    void disabledAdminStaysUnauthenticated() throws Exception {
        AdminUser admin = admin(AdminRole.SUPER_ADMIN, AdminUserStatus.DISABLED, false);

        filter.doFilter(request("/api/dashboard/summary", admin), new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void temporaryPasswordBlocksEverythingButPasswordChange() throws Exception {
        AdminUser admin = admin(AdminRole.OPERATIONS_ADMIN, AdminUserStatus.ACTIVE, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/users/riders", admin), response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void temporaryPasswordStillAllowsChangePassword() throws Exception {
        AdminUser admin = admin(AdminRole.OPERATIONS_ADMIN, AdminUserStatus.ACTIVE, true);

        filter.doFilter(request("/api/auth/change-password", admin), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        assertEquals(5L, ((AdminPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id());
    }
}
