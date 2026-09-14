package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.dto.LoginRequestDto;
import com.alvexo.adminportal.dto.LoginResponseDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
