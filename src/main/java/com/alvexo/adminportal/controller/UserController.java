package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.dto.MechanicSummaryDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.SalesAgentSummaryDto;
import com.alvexo.adminportal.dto.VehicleUserSummaryDto;
import com.alvexo.adminportal.dto.WorkshopSummaryDto;
import com.alvexo.adminportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workshop, rider and sales-agent directories all share the same allowed
 * roles (see NAV_GROUPS in dashboard-frontend/core/nav-items.ts), hence one
 * class-level @PreAuthorize for the whole controller rather than per-method.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATIONS_ADMIN','SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping("/mechanics")
    public MyApiResponse<PagedResponse<MechanicSummaryDto>> getMechanics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(userService.getMechanics(page, size, search, sortBy, sortDir));
    }

    @GetMapping("/mechanics/{id}/summary")
    public MyApiResponse<WorkshopSummaryDto> getMechanicSummary(@PathVariable Long id) {
        return MyApiResponse.success(userService.getMechanicSummary(id));
    }

    @GetMapping("/vehicle-users")
    public MyApiResponse<PagedResponse<VehicleUserSummaryDto>> getVehicleUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(userService.getVehicleUsers(page, size, search, sortBy, sortDir));
    }

    @GetMapping("/sales-agents")
    public MyApiResponse<PagedResponse<SalesAgentSummaryDto>> getSalesAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(userService.getSalesAgents(page, size, search, sortBy, sortDir));
    }
}
