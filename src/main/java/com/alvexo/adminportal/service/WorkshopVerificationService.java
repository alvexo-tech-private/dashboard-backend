package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.WorkshopVerificationDetailDto;
import com.alvexo.adminportal.dto.WorkshopVerificationSummaryDto;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AuditLog;
import com.alvexo.adminportal.entity.MechanicConfigurationSettings;
import com.alvexo.adminportal.entity.Notification;
import com.alvexo.adminportal.entity.NotificationType;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.entity.WorkshopStatus;
import com.alvexo.adminportal.entity.WorkshopVerification;
import com.alvexo.adminportal.entity.WorkshopVerificationStatus;
import com.alvexo.adminportal.entity.WorkshopVerificationType;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.AuditLogRepository;
import com.alvexo.adminportal.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.adminportal.repository.NotificationRepository;
import com.alvexo.adminportal.repository.ReferralRepository;
import com.alvexo.adminportal.repository.UserRepository;
import com.alvexo.adminportal.repository.WorkshopProfileRepository;
import com.alvexo.adminportal.repository.WorkshopVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ADMIN-US-06 — Manage Workshop Verification Queue (Platform Verified / Trust).
 *
 * Lists every workshop (request id = the mechanic's users.id), so a workshop
 * appears as NOT_SUBMITTED straight after registering, then PENDING once it
 * submits verification methods from the app. State lives in the booking
 * API's `workshop_verifications`: approving sets verified = true (creating
 * the row for a workshop verified manually without submitting). Reject and
 * request-info clear the submitted methods so the workshop resubmits from the
 * app; that table has no status/reason columns, so their outcome and reason
 * live in audit_log, which is also where "decided by" comes from.
 *
 * "Listed" is not a separate verification: it is the onboarding approval
 * handled by WorkshopRegistrationRequestService, and it is a prerequisite:
 * Platform Trust can't be approved until the workshop is Listed.
 */
@Service
@RequiredArgsConstructor
public class WorkshopVerificationService {

    private static final String ENTITY_TYPE = "WORKSHOP_VERIFICATION";
    private static final int MAX_PAGE_SIZE = 100;
    // API sort field -> User property. Status is derived, so it isn't sortable.
    private static final Map<String, String> SORT_FIELDS = Map.of("requestedAt", "createdAt", "id", "id");

    private final UserRepository userRepository;
    private final WorkshopVerificationRepository workshopVerificationRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final MechanicConfigurationSettingsRepository mechanicConfigurationSettingsRepository;
    private final ReferralRepository referralRepository;
    private final AdminUserRepository adminUserRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PagedResponse<WorkshopVerificationSummaryDto> getQueue(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<User> result = userRepository.searchVerificationQueue(normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public PagedResponse<WorkshopVerificationSummaryDto> getAll(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<User> result = userRepository.searchVerificationAll(normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public WorkshopVerificationDetailDto getById(Long mechanicId) {
        return toDetailDto(getMechanicOrThrow(mechanicId));
    }

    @Transactional
    public void approve(Long mechanicId, Long adminId) {
        User mechanic = getMechanicOrThrow(mechanicId);
        WorkshopVerificationStatus previous = guardUndecided(mechanic);
        if (!isListed(mechanic)) {
            throw ApiException.conflict("WORKSHOP_NOT_LISTED",
                    "Approve the workshop's registration before approving Platform Trust verification.");
        }

        WorkshopVerification verification = findVerification(mechanic)
                .orElseGet(() -> WorkshopVerification.builder().mechanic(mechanic).build());
        verification.setVerified(true);
        verification.setVerifiedAt(LocalDateTime.now());
        workshopVerificationRepository.save(verification);

        auditLogService.record(ENTITY_TYPE, mechanicId, adminId, previous.name(),
                WorkshopVerificationStatus.APPROVED.name(), "APPROVE", null);
        notifyDecision(mechanic, NotificationType.WORKSHOP_VERIFICATION_APPROVED,
                "Your workshop's Platform Trust request has been approved.");
    }

    @Transactional
    public void reject(Long mechanicId, Long adminId, String reason) {
        User mechanic = getMechanicOrThrow(mechanicId);
        WorkshopVerificationStatus previous = guardUndecided(mechanic);
        clearSubmission(mechanic);

        auditLogService.record(ENTITY_TYPE, mechanicId, adminId, previous.name(),
                WorkshopVerificationStatus.REJECTED.name(), "REJECT", reason);
        notifyDecision(mechanic, NotificationType.WORKSHOP_VERIFICATION_REJECTED,
                "Your workshop's Platform Trust request has been rejected. Reason: " + reason);
    }

    @Transactional
    public void requestInfo(Long mechanicId, Long adminId, String comments) {
        User mechanic = getMechanicOrThrow(mechanicId);
        WorkshopVerificationStatus previous = guardUndecided(mechanic);
        clearSubmission(mechanic);

        auditLogService.record(ENTITY_TYPE, mechanicId, adminId, previous.name(),
                WorkshopVerificationStatus.ADDITIONAL_INFO_REQUESTED.name(), "REQUEST_INFO", comments);
        notifyDecision(mechanic, NotificationType.WORKSHOP_VERIFICATION_INFO_REQUESTED,
                "Additional information is required for your workshop's Platform Trust request: " + comments);
    }

    private WorkshopVerificationStatus guardUndecided(User mechanic) {
        WorkshopVerificationStatus status = statusOf(mechanic, findVerification(mechanic));
        if (status == WorkshopVerificationStatus.APPROVED) {
            throw ApiException.conflict("ALREADY_DECIDED", "This workshop is already Platform Verified.");
        }
        return status;
    }

    private void clearSubmission(User mechanic) {
        findVerification(mechanic).ifPresent(verification -> {
            verification.getCompletedMethods().clear();
            verification.setVerified(false);
            verification.setVerifiedAt(null);
            workshopVerificationRepository.save(verification);
        });
    }

    private WorkshopVerificationStatus statusOf(User mechanic, Optional<WorkshopVerification> verification) {
        if (verification.map(v -> Boolean.TRUE.equals(v.getVerified())).orElse(false)) {
            return WorkshopVerificationStatus.APPROVED;
        }
        if (verification.map(v -> !v.getCompletedMethods().isEmpty()).orElse(false)) {
            return WorkshopVerificationStatus.PENDING;
        }
        return latestDecision(mechanic)
                .map(log -> "REJECT".equals(log.getAction()) ? WorkshopVerificationStatus.REJECTED
                        : "REQUEST_INFO".equals(log.getAction()) ? WorkshopVerificationStatus.ADDITIONAL_INFO_REQUESTED
                        : WorkshopVerificationStatus.NOT_SUBMITTED)
                .orElse(WorkshopVerificationStatus.NOT_SUBMITTED);
    }

    /** Listed = registration approved (profile VERIFIED, or SUSPENDED after having been verified). */
    private boolean isListed(User mechanic) {
        return workshopProfileRepository.findByMechanicId(mechanic.getId())
                .map(p -> p.getStatus() == WorkshopStatus.VERIFIED || p.getStatus() == WorkshopStatus.SUSPENDED)
                .orElse(false);
    }

    private Optional<WorkshopVerification> findVerification(User mechanic) {
        return workshopVerificationRepository.findByMechanicId(mechanic.getId());
    }

    private Optional<AuditLog> latestDecision(User mechanic) {
        return auditLogRepository.findFirstByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(ENTITY_TYPE, mechanic.getId());
    }

    /** When the workshop last submitted methods, otherwise when it registered. */
    private LocalDateTime requestedAt(User mechanic, Optional<WorkshopVerification> verification) {
        return verification.filter(v -> !v.getCompletedMethods().isEmpty())
                .map(WorkshopVerification::getUpdatedAt)
                .orElse(mechanic.getCreatedAt());
    }

    private void notifyDecision(User mechanic, NotificationType type, String message) {
        notificationRepository.save(Notification.builder()
                .user(mechanic)
                .title("Workshop Verification Update")
                .message(message)
                .notificationType(type)
                .isRead(false)
                .relatedEntityType(ENTITY_TYPE)
                .relatedEntityId(mechanic.getId())
                .build());

        referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .ifPresent(referral -> notificationRepository.save(Notification.builder()
                        .user(referral.getSalesRep())
                        .title("Referred Workshop Verification Update")
                        .message("Workshop " + mechanic.getWorkshopName() + ": " + message)
                        .notificationType(type)
                        .isRead(false)
                        .relatedEntityType(ENTITY_TYPE)
                        .relatedEntityId(mechanic.getId())
                        .build()));
    }

    private User getMechanicOrThrow(Long mechanicId) {
        return userRepository.findById(mechanicId)
                .filter(u -> u.getRole() == UserRole.MECHANIC && !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> ApiException.notFound("Verification request not found."));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String sortField = SORT_FIELDS.getOrDefault(sortBy, "createdAt");
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));
    }

    private String normalize(String search) {
        return search == null ? "" : search.trim();
    }

    private String accountStatus(User user) {
        if (Boolean.TRUE.equals(user.getDeleted())) return "DELETED";
        if (!Boolean.TRUE.equals(user.getActive())) return "SUSPENDED";
        return "ACTIVE";
    }

    private WorkshopVerificationSummaryDto toSummaryDto(User mechanic) {
        Optional<WorkshopVerification> verification = findVerification(mechanic);
        return WorkshopVerificationSummaryDto.builder()
                .id(mechanic.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .city(mechanic.getCity())
                .verificationType(WorkshopVerificationType.TRUST)
                .status(statusOf(mechanic, verification))
                .requestedAt(requestedAt(mechanic, verification))
                .listed(isListed(mechanic))
                .build();
    }

    private WorkshopVerificationDetailDto toDetailDto(User mechanic) {
        Optional<WorkshopVerification> verification = findVerification(mechanic);
        MechanicConfigurationSettings settings = mechanicConfigurationSettingsRepository.findByMechanicId(mechanic.getId()).orElse(null);
        String salesAgentName = referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .map(referral -> referral.getSalesRep().getFirstName() + " " + referral.getSalesRep().getLastName())
                .orElse(null);
        WorkshopVerificationStatus status = statusOf(mechanic, verification);
        boolean decided = status != WorkshopVerificationStatus.NOT_SUBMITTED && status != WorkshopVerificationStatus.PENDING;
        Optional<AuditLog> decision = decided ? latestDecision(mechanic) : Optional.empty();
        String decidedByAdminName = decision
                .flatMap(log -> adminUserRepository.findById(log.getPerformedByAdminId()))
                .map(AdminUser::getFullName)
                .orElse(null);

        return WorkshopVerificationDetailDto.builder()
                .id(mechanic.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .addressLine1(mechanic.getAddressLine1())
                .addressLine2(mechanic.getAddressLine2())
                .city(mechanic.getCity())
                .state(mechanic.getState())
                .postalCode(mechanic.getPostalCode())
                .country(mechanic.getCountry())
                .verificationType(WorkshopVerificationType.TRUST)
                .status(status)
                .requestedAt(requestedAt(mechanic, verification))
                .listed(isListed(mechanic))
                .completedMethods(verification.map(WorkshopVerification::getCompletedMethods).orElse(Set.of()))
                .registeredAt(mechanic.getCreatedAt())
                .accountStatus(accountStatus(mechanic))
                .rating(mechanic.getRating())
                .totalReviews(mechanic.getTotalReviews())
                .totalBookingsCompleted(mechanic.getTotalBookingsCompleted())
                .pickupDropEnabled(settings != null && Boolean.TRUE.equals(settings.getPickupDropFacilityEnabled()))
                .advancePaymentEnabled(settings != null && Boolean.TRUE.equals(settings.getRepairsRequireAdvance()))
                .salesAgentName(salesAgentName)
                .decisionReason(decision.map(AuditLog::getReason).orElse(null))
                .decidedByAdminName(decidedByAdminName)
                .decidedAt(status == WorkshopVerificationStatus.APPROVED
                        ? verification.map(WorkshopVerification::getVerifiedAt).orElse(null)
                        : decision.map(AuditLog::getCreatedAt).orElse(null))
                .build();
    }
}
