package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.config.AdminPrincipal;
import com.alvexo.adminportal.dto.AccountHistoryEntryDto;
import com.alvexo.adminportal.dto.AdminUserDto;
import com.alvexo.adminportal.dto.ChangeAdminRoleRequestDto;
import com.alvexo.adminportal.dto.CreateAdminUserRequestDto;
import com.alvexo.adminportal.dto.DecisionRequestDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.TemporaryPasswordDto;
import com.alvexo.adminportal.entity.AdminRole;
import com.alvexo.adminportal.entity.AdminUserStatus;
import com.alvexo.adminportal.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin users and roles (ADMIN-US-29). Super Admin only (BR-04, BR-11). */
@RestController
@RequestMapping("/api/admin-users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public MyApiResponse<PagedResponse<AdminUserDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) AdminRole role,
            @RequestParam(required = false) AdminUserStatus status,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(adminUserService.list(page, size, search, role, status, sortBy, sortDir));
    }

    @PostMapping
    public MyApiResponse<TemporaryPasswordDto> create(@Valid @RequestBody CreateAdminUserRequestDto request,
                                                      @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(adminUserService.create(request.getEmail(), request.getFullName(),
                request.getRole(), principal.id(), request.getReason()), "Admin user created.");
    }

    @PostMapping("/{id}/role")
    public MyApiResponse<AdminUserDto> changeRole(@PathVariable Long id,
                                                  @Valid @RequestBody ChangeAdminRoleRequestDto request,
                                                  @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(adminUserService.changeRole(id, request.getRole(), principal.id(),
                request.getReason()), "Role changed.");
    }

    @PostMapping("/{id}/disable")
    public MyApiResponse<AdminUserDto> disable(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                               @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(adminUserService.disable(id, principal.id(), request.getReason()),
                "Admin user disabled.");
    }

    @PostMapping("/{id}/enable")
    public MyApiResponse<AdminUserDto> enable(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                              @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(adminUserService.enable(id, principal.id(), request.getReason()),
                "Admin user enabled.");
    }

    @PostMapping("/{id}/reset-password")
    public MyApiResponse<TemporaryPasswordDto> resetPassword(@PathVariable Long id,
                                                             @Valid @RequestBody DecisionRequestDto request,
                                                             @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(adminUserService.resetPassword(id, principal.id(), request.getReason()),
                "Password reset.");
    }

    @GetMapping("/{id}/history")
    public MyApiResponse<PagedResponse<AccountHistoryEntryDto>> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return MyApiResponse.success(adminUserService.getHistory(id, page, size));
    }
}
