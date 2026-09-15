package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.config.AdminPrincipal;
import com.alvexo.adminportal.dto.DecisionRequestDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.WorkshopVerificationDetailDto;
import com.alvexo.adminportal.dto.WorkshopVerificationSummaryDto;
import com.alvexo.adminportal.entity.WorkshopVerificationType;
import com.alvexo.adminportal.service.WorkshopVerificationService;
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
 * ADMIN-US-06 — DoD: "Only Operations Admin and Super Admin can process
 * verification requests. Finance Admin cannot approve workshop verification."
 */
@RestController
@RequestMapping("/api/workshop-verifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATIONS_ADMIN','SUPER_ADMIN')")
public class WorkshopVerificationController {

    private final WorkshopVerificationService workshopVerificationService;

    @GetMapping("/queue")
    public MyApiResponse<PagedResponse<WorkshopVerificationSummaryDto>> getQueue(
            @RequestParam(required = false) WorkshopVerificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "requestedAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return MyApiResponse.success(workshopVerificationService.getQueue(type, page, size, search, sortBy, sortDir));
    }

    @GetMapping
    public MyApiResponse<PagedResponse<WorkshopVerificationSummaryDto>> getAll(
            @RequestParam(required = false) WorkshopVerificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "requestedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return MyApiResponse.success(workshopVerificationService.getAll(type, page, size, search, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public MyApiResponse<WorkshopVerificationDetailDto> getById(@PathVariable Long id) {
        return MyApiResponse.success(workshopVerificationService.getById(id));
    }

    @PostMapping("/{id}/approve")
    public MyApiResponse<Void> approve(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        workshopVerificationService.approve(id, principal.id());
        return MyApiResponse.success(null, "Verification request approved.");
    }

    @PostMapping("/{id}/reject")
    public MyApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                       @AuthenticationPrincipal AdminPrincipal principal) {
        workshopVerificationService.reject(id, principal.id(), request.getReason());
        return MyApiResponse.success(null, "Verification request rejected.");
    }

    @PostMapping("/{id}/request-info")
    public MyApiResponse<Void> requestInfo(@PathVariable Long id, @Valid @RequestBody DecisionRequestDto request,
                                            @AuthenticationPrincipal AdminPrincipal principal) {
        workshopVerificationService.requestInfo(id, principal.id(), request.getReason());
        return MyApiResponse.success(null, "Additional information requested.");
    }

    @PostMapping("/{id}/mark-resubmitted")
    public MyApiResponse<Void> markResubmitted(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        workshopVerificationService.markResubmitted(id, principal.id());
        return MyApiResponse.success(null, "Marked as resubmitted.");
    }
}
