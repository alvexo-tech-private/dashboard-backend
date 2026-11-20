package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.config.AdminPrincipal;
import com.alvexo.adminportal.dto.AccountHistoryEntryDto;
import com.alvexo.adminportal.dto.DecisionRequestDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.entity.AccountStatus;
import com.alvexo.adminportal.service.AccountLifecycleService;
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

/**
 * Account lifecycle for riders, workshops (mechanics) and sales agents.
 * ADMIN-US-04 BR-05: only Operations Admin and Super Admin. Every action
 * requires a reason and is written to the audit log.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATIONS_ADMIN','SUPER_ADMIN')")
public class AccountController {

    private final AccountLifecycleService accountLifecycleService;

    @PostMapping("/{id}/suspend")
    public MyApiResponse<AccountStatus> suspend(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                                @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(accountLifecycleService.suspend(id, principal.id(), request.getReason()),
                "Account suspended.");
    }

    @PostMapping("/{id}/restore")
    public MyApiResponse<AccountStatus> restore(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                                @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(accountLifecycleService.restore(id, principal.id(), request.getReason()),
                "Account restored.");
    }

    @PostMapping("/{id}/delete")
    public MyApiResponse<AccountStatus> delete(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                               @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(accountLifecycleService.delete(id, principal.id(), request.getReason()),
                "Account deleted.");
    }

    @GetMapping("/{id}/history")
    public MyApiResponse<PagedResponse<AccountHistoryEntryDto>> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return MyApiResponse.success(accountLifecycleService.getHistory(id, page, size));
    }
}
