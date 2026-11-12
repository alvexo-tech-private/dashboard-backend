package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.WorkshopRegistrationRequestDetailDto;
import com.alvexo.adminportal.dto.WorkshopRegistrationRequestSummaryDto;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.Notification;
import com.alvexo.adminportal.entity.NotificationType;
import com.alvexo.adminportal.entity.OnboardingDecision;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.WorkshopOnboarding;
import com.alvexo.adminportal.entity.WorkshopProfile;
import com.alvexo.adminportal.entity.WorkshopRegistrationRequestStatus;
import com.alvexo.adminportal.entity.WorkshopStatus;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.AuditLogRepository;
import com.alvexo.adminportal.repository.NotificationRepository;
import com.alvexo.adminportal.repository.ReferralRepository;
import com.alvexo.adminportal.repository.WorkshopOnboardingRepository;
import com.alvexo.adminportal.repository.WorkshopProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * ADMIN-US-03 — Manage Workshop Registration Requests.
 *
 * A registration request is a workshop's submitted onboarding in the booking
 * API's own tables (workshop_onboardings + workshop_profiles), so a decision
 * taken here is what the workshop app's Current Status card and the rider
 * booking gate see. Request ids are workshop_onboardings ids.
 *
 * Decisions mirror the booking API's WorkshopProfileService.recordOnboardingDecision:
 * the onboarding row records decision/reason/time and the profile status moves
 * to VERIFIED (Self Listed), REJECTED or IN_PROGRESS (more info needed). A
 * workshop asked for more info resubmits from the app, which puts it back to
 * PENDING, so there is no admin-side "mark resubmitted" step.
 */
@Service
@RequiredArgsConstructor
public class WorkshopRegistrationRequestService {

    private static final String ENTITY_TYPE = "WORKSHOP_REGISTRATION_REQUEST";
    private static final int MAX_PAGE_SIZE = 100;
    // API sort field -> WorkshopOnboarding property. Status is derived, so it isn't sortable.
    private static final Map<String, String> SORT_FIELDS = Map.of("registeredAt", "submittedAt", "id", "id");
    private static final Set<WorkshopRegistrationRequestStatus> TERMINAL_STATUSES =
            Set.of(WorkshopRegistrationRequestStatus.APPROVED, WorkshopRegistrationRequestStatus.REJECTED);

    private final WorkshopOnboardingRepository workshopOnboardingRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final ReferralRepository referralRepository;
    private final AdminUserRepository adminUserRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PagedResponse<WorkshopRegistrationRequestSummaryDto> getQueue(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<WorkshopOnboarding> result = workshopOnboardingRepository.searchQueue(normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkshopRegistrationRequestSummaryDto> getAll(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<WorkshopOnboarding> result = workshopOnboardingRepository.searchAll(normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public WorkshopRegistrationRequestDetailDto getById(Long id) {
        return toDetailDto(getOrThrow(id));
    }

    @Transactional
    public void approve(Long id, Long adminId) {
        WorkshopOnboarding onboarding = decide(id, adminId, OnboardingDecision.APPROVED, null, "APPROVE");
        notifyDecision(onboarding, NotificationType.WORKSHOP_REGISTRATION_APPROVED,
                "Your workshop registration has been approved.");
    }

    @Transactional
    public void reject(Long id, Long adminId, String reason) {
        WorkshopOnboarding onboarding = decide(id, adminId, OnboardingDecision.REJECTED, reason, "REJECT");
        notifyDecision(onboarding, NotificationType.WORKSHOP_REGISTRATION_REJECTED,
                "Your workshop registration has been rejected. Reason: " + reason);
    }

    @Transactional
    public void requestInfo(Long id, Long adminId, String comments) {
        WorkshopOnboarding onboarding = decide(id, adminId, OnboardingDecision.MORE_INFO, comments, "REQUEST_INFO");
        notifyDecision(onboarding, NotificationType.WORKSHOP_REGISTRATION_INFO_REQUESTED,
                "Additional information is required for your workshop registration: " + comments);
    }

    private WorkshopOnboarding decide(Long id, Long adminId, OnboardingDecision decision, String reason, String action) {
        WorkshopOnboarding onboarding = getOrThrow(id);
        WorkshopProfile profile = getProfileOrThrow(onboarding);
        WorkshopRegistrationRequestStatus previous = statusOf(onboarding, profile);
        if (TERMINAL_STATUSES.contains(previous)) {
            throw ApiException.conflict("ALREADY_DECIDED", "This registration request has already been decided.");
        }

        onboarding.setDecision(decision);
        onboarding.setDecisionReason(reason);
        onboarding.setDecidedAt(LocalDateTime.now());
        workshopOnboardingRepository.save(onboarding);

        profile.setStatus(switch (decision) {
            case APPROVED -> WorkshopStatus.VERIFIED;
            case REJECTED -> WorkshopStatus.REJECTED;
            case MORE_INFO -> WorkshopStatus.IN_PROGRESS;
        });
        workshopProfileRepository.save(profile);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), statusOf(onboarding, profile).name(), action, reason);
        return onboarding;
    }

    /** Maps the booking API's profile status onto the four statuses the admin portal shows. */
    private WorkshopRegistrationRequestStatus statusOf(WorkshopOnboarding onboarding, WorkshopProfile profile) {
        return switch (profile.getStatus()) {
            case VERIFIED, SUSPENDED -> WorkshopRegistrationRequestStatus.APPROVED;
            case REJECTED -> WorkshopRegistrationRequestStatus.REJECTED;
            case IN_PROGRESS -> onboarding.getDecision() == OnboardingDecision.MORE_INFO
                    ? WorkshopRegistrationRequestStatus.ADDITIONAL_INFO_REQUESTED
                    : WorkshopRegistrationRequestStatus.PENDING;
            case PENDING, NEW_TO_APP -> WorkshopRegistrationRequestStatus.PENDING;
        };
    }

    private void notifyDecision(WorkshopOnboarding onboarding, NotificationType type, String message) {
        User mechanic = onboarding.getMechanic();
        notificationRepository.save(Notification.builder()
                .user(mechanic)
                .title("Workshop Registration Update")
                .message(message)
                .notificationType(type)
                .isRead(false)
                .relatedEntityType(ENTITY_TYPE)
                .relatedEntityId(onboarding.getId())
                .build());

        referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .ifPresent(referral -> notificationRepository.save(Notification.builder()
                        .user(referral.getSalesRep())
                        .title("Referred Workshop Registration Update")
                        .message("Workshop " + mechanic.getWorkshopName() + ": " + message)
                        .notificationType(type)
                        .isRead(false)
                        .relatedEntityType(ENTITY_TYPE)
                        .relatedEntityId(onboarding.getId())
                        .build()));
    }

    private WorkshopOnboarding getOrThrow(Long id) {
        return workshopOnboardingRepository.findById(id)
                .filter(o -> Boolean.TRUE.equals(o.getSubmitted()))
                .orElseThrow(() -> ApiException.notFound("Registration request not found."));
    }

    private WorkshopProfile getProfileOrThrow(WorkshopOnboarding onboarding) {
        return workshopProfileRepository.findByMechanicId(onboarding.getMechanic().getId())
                .orElseThrow(() -> ApiException.notFound("Workshop profile not found."));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String sortField = SORT_FIELDS.getOrDefault(sortBy, "id");
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));
    }

    private String normalize(String search) {
        return search == null ? "" : search.trim();
    }

    private WorkshopRegistrationRequestSummaryDto toSummaryDto(WorkshopOnboarding onboarding) {
        User mechanic = onboarding.getMechanic();
        return WorkshopRegistrationRequestSummaryDto.builder()
                .id(onboarding.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .city(mechanic.getCity())
                .registeredAt(onboarding.getSubmittedAt())
                .status(statusOf(onboarding, getProfileOrThrow(onboarding)))
                .build();
    }

    private WorkshopRegistrationRequestDetailDto toDetailDto(WorkshopOnboarding onboarding) {
        User mechanic = onboarding.getMechanic();
        String referredSalesAgentName = referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .map(referral -> referral.getSalesRep().getFirstName() + " " + referral.getSalesRep().getLastName())
                .orElse(null);
        // The booking API's tables don't record who decided, so it comes from our own audit trail.
        String decidedByAdminName = onboarding.getDecision() == null ? null
                : auditLogRepository.findFirstByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(ENTITY_TYPE, onboarding.getId())
                        .flatMap(log -> adminUserRepository.findById(log.getPerformedByAdminId()))
                        .map(AdminUser::getFullName)
                        .orElse(null);

        return WorkshopRegistrationRequestDetailDto.builder()
                .id(onboarding.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .addressLine1(mechanic.getAddressLine1())
                .addressLine2(mechanic.getAddressLine2())
                .city(mechanic.getCity())
                .state(mechanic.getState())
                .postalCode(mechanic.getPostalCode())
                .country(mechanic.getCountry())
                .registeredAt(onboarding.getSubmittedAt())
                .referredSalesAgentName(referredSalesAgentName)
                .status(statusOf(onboarding, getProfileOrThrow(onboarding)))
                .decisionReason(onboarding.getDecisionReason())
                .decidedByAdminName(decidedByAdminName)
                .decidedAt(onboarding.getDecidedAt())
                .build();
    }
}
