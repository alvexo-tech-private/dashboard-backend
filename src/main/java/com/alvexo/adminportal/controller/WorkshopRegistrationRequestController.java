package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.config.AdminPrincipal;
import com.alvexo.adminportal.dto.DecisionRequestDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.WorkshopRegistrationRequestDetailDto;
import com.alvexo.adminportal.dto.WorkshopRegistrationRequestSummaryDto;
import com.alvexo.adminportal.service.WorkshopRegistrationRequestService;
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

@RestController
@RequestMapping("/api/workshop-registration-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATIONS_ADMIN','SUPER_ADMIN')")
public class WorkshopRegistrationRequestController {

    private final WorkshopRegistrationRequestService workshopRegistrationRequestService;

    @GetMapping("/queue")
    public MyApiResponse<PagedResponse<WorkshopRegistrationRequestSummaryDto>> getQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "registeredAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(workshopRegistrationRequestService.getQueue(page, size, search, sortBy, sortDir));
    }

    @GetMapping
    public MyApiResponse<PagedResponse<WorkshopRegistrationRequestSummaryDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "registeredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return MyApiResponse.success(workshopRegistrationRequestService.getAll(page, size, search, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public MyApiResponse<WorkshopRegistrationRequestDetailDto> getById(@PathVariable Long id) {
        return MyApiResponse.success(workshopRegistrationRequestService.getById(id));
    }

    @PostMapping("/{id}/approve")
    public MyApiResponse<Void> approve(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        workshopRegistrationRequestService.approve(id, principal.id());
        return MyApiResponse.success(null, "Registration request approved.");
    }

    @PostMapping("/{id}/reject")
    public MyApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                       @AuthenticationPrincipal AdminPrincipal principal) {
        workshopRegistrationRequestService.reject(id, principal.id(), request.getReason());
        return MyApiResponse.success(null, "Registration request rejected.");
    }

    @PostMapping("/{id}/request-info")
    public MyApiResponse<Void> requestInfo(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                            @AuthenticationPrincipal AdminPrincipal principal) {
        workshopRegistrationRequestService.requestInfo(id, principal.id(), request.getReason());
        return MyApiResponse.success(null, "Additional information requested.");
    }

    @PostMapping("/{id}/mark-resubmitted")
    public MyApiResponse<Void> markResubmitted(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        workshopRegistrationRequestService.markResubmitted(id, principal.id());
        return MyApiResponse.success(null, "Marked as resubmitted.");
    }
}
