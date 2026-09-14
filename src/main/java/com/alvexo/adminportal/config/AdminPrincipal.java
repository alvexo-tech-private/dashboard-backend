package com.alvexo.adminportal.config;

import com.alvexo.adminportal.entity.AdminRole;

/** The authenticated principal set on the SecurityContext by JwtAuthenticationFilter, built entirely from JWT claims (no per-request DB hit). */
public record AdminPrincipal(Long id, String email, String fullName, AdminRole role) {
}
