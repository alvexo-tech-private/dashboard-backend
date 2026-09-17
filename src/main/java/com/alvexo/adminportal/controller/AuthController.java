package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.config.AdminPrincipal;
import com.alvexo.adminportal.dto.ChangePasswordRequestDto;
import com.alvexo.adminportal.dto.LoginRequestDto;
import com.alvexo.adminportal.dto.LoginResponseDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public MyApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return MyApiResponse.success(authService.login(request.getEmail(), request.getPassword()));
    }

    // Authenticated for any admin role — required token comes from
    // SecurityConfig's default "anyRequest().authenticated()" rule, since
    // every admin must be able to change their own password.
    @PostMapping("/change-password")
    public MyApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request,
                                               @AuthenticationPrincipal AdminPrincipal principal) {
        authService.changePassword(principal.id(), request.getCurrentPassword(), request.getNewPassword());
        return MyApiResponse.success(null, "Password updated successfully.");
    }
}
