package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.AdminUserSummaryDto;
import com.alvexo.adminportal.dto.LoginResponseDto;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AdminUserStatus;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponseDto login(String email, String password) {
        AdminUser admin = adminUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE);
        }
        if (admin.getStatus() != AdminUserStatus.ACTIVE) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE);
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(admin);

        String token = jwtService.generateAccessToken(admin);
        return LoginResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .adminUser(AdminUserSummaryDto.builder()
                        .id(admin.getId())
                        .email(admin.getEmail())
                        .fullName(admin.getFullName())
                        .role(admin.getRole())
                        .build())
                .build();
    }
}
