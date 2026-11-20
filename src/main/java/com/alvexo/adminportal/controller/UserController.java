package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.dto.BookingRowDto;
import com.alvexo.adminportal.dto.MechanicSummaryDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.ReferralRowDto;
import com.alvexo.adminportal.dto.ReviewRowDto;
import com.alvexo.adminportal.dto.RiderDetailDto;
import com.alvexo.adminportal.dto.SalesAgentDetailDto;
import com.alvexo.adminportal.dto.SalesAgentSummaryDto;
import com.alvexo.adminportal.dto.VehicleUserSummaryDto;
import com.alvexo.adminportal.dto.WorkshopSummaryDto;
import com.alvexo.adminportal.service.PeopleDetailService;
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
    private final PeopleDetailService peopleDetailService;

    @GetMapping("/mechanics")
    public MyApiResponse<PagedResponse<MechanicSummaryDto>> getMechanics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(userService.getMechanics(page, size, search, status, sortBy, sortDir));
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
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(userService.getVehicleUsers(page, size, search, status, sortBy, sortDir));
    }

    @GetMapping("/sales-agents")
    public MyApiResponse<PagedResponse<SalesAgentSummaryDto>> getSalesAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(userService.getSalesAgents(page, size, search, status, sortBy, sortDir));
    }

    // ---- Detail pages (ADMIN-US-08/09/10), read-only ----

    @GetMapping("/vehicle-users/{id}")
    public MyApiResponse<RiderDetailDto> getRider(@PathVariable Long id) {
        return MyApiResponse.success(peopleDetailService.getRider(id));
    }

    @GetMapping("/sales-agents/{id}")
    public MyApiResponse<SalesAgentDetailDto> getSalesAgent(@PathVariable Long id) {
        return MyApiResponse.success(peopleDetailService.getSalesAgent(id));
    }

    @GetMapping("/sales-agents/{id}/referrals")
    public MyApiResponse<PagedResponse<ReferralRowDto>> getReferrals(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return MyApiResponse.success(peopleDetailService.getReferrals(id, page, size));
    }

    /** Booking history of a rider or a workshop; status = ALL or one BookingStatus. */
    @GetMapping("/{id}/bookings")
    public MyApiResponse<PagedResponse<BookingRowDto>> getBookings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String status) {
        return MyApiResponse.success(peopleDetailService.getBookings(id, page, size, status));
    }

    /** Reviews written by a rider, or received by a workshop. */
    @GetMapping("/{id}/reviews")
    public MyApiResponse<PagedResponse<ReviewRowDto>> getReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return MyApiResponse.success(peopleDetailService.getReviews(id, page, size));
    }
}
