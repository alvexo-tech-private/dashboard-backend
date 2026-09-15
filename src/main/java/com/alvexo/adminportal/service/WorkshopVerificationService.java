package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.WorkshopVerificationDetailDto;
import com.alvexo.adminportal.dto.WorkshopVerificationSummaryDto;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.MechanicConfigurationSettings;
import com.alvexo.adminportal.entity.Notification;
import com.alvexo.adminportal.entity.NotificationType;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.entity.WorkshopVerification;
import com.alvexo.adminportal.entity.WorkshopVerificationStatus;
import com.alvexo.adminportal.entity.WorkshopVerificationType;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.adminportal.repository.NotificationRepository;
import com.alvexo.adminportal.repository.ReferralRepository;
import com.alvexo.adminportal.repository.UserRepository;
import com.alvexo.adminportal.repository.WorkshopVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * ADMIN-US-06 — Manage Workshop Verification Queue.
 *
 * Mirrors WorkshopRegistrationRequestService's lazy-sync approach: this
 * service owns workshop_verifications, but nothing in the sibling app ever
 * inserts a row here directly, so every read syncs first — one PENDING row
 * per (active mechanic, verification type) pair that doesn't already have
 * one. See WorkshopVerification's Javadoc for why requestedAt is copied
 * from the mechanic's own registration date.
 *
 * "Approved Listed or Trust status shall update the corresponding workshop
 * status" (BR-15) is satisfied by this row's own `status` field — there is
 * no separate boolean on the workshop itself to flip; the dashboard's
 * Listed/Platform Trusted counts and the workshop directory read directly
 * from APPROVED rows here.
 */
@Service
@RequiredArgsConstructor
public class WorkshopVerificationService {

    private static final String ENTITY_TYPE = "WORKSHOP_VERIFICATION";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_FIELDS = Set.of("requestedAt", "status", "verificationType", "id");
    private static final Set<WorkshopVerificationStatus> TERMINAL_STATUSES =
            Set.of(WorkshopVerificationStatus.APPROVED, WorkshopVerificationStatus.REJECTED);

    private final WorkshopVerificationRepository workshopVerificationRepository;
    private final UserRepository userRepository;
    private final MechanicConfigurationSettingsRepository mechanicConfigurationSettingsRepository;
    private final ReferralRepository referralRepository;
    private final AdminUserRepository adminUserRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PagedResponse<WorkshopVerificationSummaryDto> getQueue(WorkshopVerificationType type, int page, int size,
                                                                    String search, String sortBy, String sortDir) {
        syncNewRequests();
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<WorkshopVerification> result = workshopVerificationRepository.searchQueue(type, normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    @Transactional
    public PagedResponse<WorkshopVerificationSummaryDto> getAll(WorkshopVerificationType type, int page, int size,
                                                                  String search, String sortBy, String sortDir) {
        syncNewRequests();
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<WorkshopVerification> result = workshopVerificationRepository.searchAll(type, normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    public WorkshopVerificationDetailDto getById(Long id) {
        return toDetailDto(getOrThrow(id));
    }

    @Transactional
    public void approve(Long id, Long adminId) {
        WorkshopVerification verification = getOrThrow(id);
        guardNotTerminal(verification);
        WorkshopVerificationStatus previous = verification.getStatus();

        verification.setStatus(WorkshopVerificationStatus.APPROVED);
        verification.setDecidedByAdminId(adminId);
        verification.setDecidedAt(LocalDateTime.now());
        workshopVerificationRepository.save(verification);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), verification.getStatus().name(), "APPROVE", null);
        notifyDecision(verification, NotificationType.WORKSHOP_VERIFICATION_APPROVED,
                label(verification) + " has been approved.");
    }

    @Transactional
    public void reject(Long id, Long adminId, String reason) {
        WorkshopVerification verification = getOrThrow(id);
        guardNotTerminal(verification);
        WorkshopVerificationStatus previous = verification.getStatus();

        verification.setStatus(WorkshopVerificationStatus.REJECTED);
        verification.setDecisionReason(reason);
        verification.setDecidedByAdminId(adminId);
        verification.setDecidedAt(LocalDateTime.now());
        workshopVerificationRepository.save(verification);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), verification.getStatus().name(), "REJECT", reason);
        notifyDecision(verification, NotificationType.WORKSHOP_VERIFICATION_REJECTED,
                label(verification) + " has been rejected. Reason: " + reason);
    }

    @Transactional
    public void requestInfo(Long id, Long adminId, String comments) {
        WorkshopVerification verification = getOrThrow(id);
        guardNotTerminal(verification);
        WorkshopVerificationStatus previous = verification.getStatus();

        verification.setStatus(WorkshopVerificationStatus.ADDITIONAL_INFO_REQUESTED);
        verification.setDecisionReason(comments);
        verification.setDecidedByAdminId(adminId);
        verification.setDecidedAt(LocalDateTime.now());
        workshopVerificationRepository.save(verification);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), verification.getStatus().name(), "REQUEST_INFO", comments);
        notifyDecision(verification, NotificationType.WORKSHOP_VERIFICATION_INFO_REQUESTED,
                "Additional information is required for " + label(verification) + ": " + comments);
    }

    /**
     * Interim workaround, same as WorkshopRegistrationRequestService: there is
     * no automated resubmission channel, so an Operations Admin manually
     * confirms information was received offline and moves this back to
     * Pending for re-review.
     */
    @Transactional
    public void markResubmitted(Long id, Long adminId) {
        WorkshopVerification verification = getOrThrow(id);
        if (verification.getStatus() != WorkshopVerificationStatus.ADDITIONAL_INFO_REQUESTED) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "Only requests currently Additional Information Requested can be marked resubmitted.");
        }
        WorkshopVerificationStatus previous = verification.getStatus();
        verification.setStatus(WorkshopVerificationStatus.PENDING);
        workshopVerificationRepository.save(verification);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), verification.getStatus().name(), "MARK_RESUBMITTED", null);
    }

    private void syncNewRequests() {
        List<User> mechanics = userRepository.findByRoleAndDeletedFalse(UserRole.MECHANIC);
        for (WorkshopVerificationType type : WorkshopVerificationType.values()) {
            Set<Long> existingMechanicIds = workshopVerificationRepository.findAllMechanicIdsByVerificationType(type);
            for (User mechanic : mechanics) {
                if (!existingMechanicIds.contains(mechanic.getId())) {
                    workshopVerificationRepository.save(WorkshopVerification.builder()
                            .mechanic(mechanic)
                            .verificationType(type)
                            .status(WorkshopVerificationStatus.PENDING)
                            .requestedAt(mechanic.getCreatedAt())
                            .build());
                }
            }
        }
    }

    private void guardNotTerminal(WorkshopVerification verification) {
        if (TERMINAL_STATUSES.contains(verification.getStatus())) {
            throw ApiException.conflict("ALREADY_DECIDED", "This verification request has already been decided.");
        }
    }

    private String label(WorkshopVerification verification) {
        String type = verification.getVerificationType() == WorkshopVerificationType.TRUST ? "Platform Trust" : "Listed status";
        return "Your workshop's " + type + " request";
    }

    private void notifyDecision(WorkshopVerification verification, NotificationType type, String message) {
        User mechanic = verification.getMechanic();
        notificationRepository.save(Notification.builder()
                .user(mechanic)
                .title("Workshop Verification Update")
                .message(message)
                .notificationType(type)
                .isRead(false)
                .relatedEntityType(ENTITY_TYPE)
                .relatedEntityId(verification.getId())
                .build());

        referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .ifPresent(referral -> notificationRepository.save(Notification.builder()
                        .user(referral.getSalesRep())
                        .title("Referred Workshop Verification Update")
                        .message("Workshop " + mechanic.getWorkshopName() + ": " + message)
                        .notificationType(type)
                        .isRead(false)
                        .relatedEntityType(ENTITY_TYPE)
                        .relatedEntityId(verification.getId())
                        .build()));
    }

    private WorkshopVerification getOrThrow(Long id) {
        return workshopVerificationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Verification request not found."));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String sortField = SORT_FIELDS.contains(sortBy) ? sortBy : "requestedAt";
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

    private WorkshopVerificationSummaryDto toSummaryDto(WorkshopVerification verification) {
        User mechanic = verification.getMechanic();
        return WorkshopVerificationSummaryDto.builder()
                .id(verification.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .city(mechanic.getCity())
                .verificationType(verification.getVerificationType())
                .status(verification.getStatus())
                .requestedAt(verification.getRequestedAt())
                .build();
    }

    private WorkshopVerificationDetailDto toDetailDto(WorkshopVerification verification) {
        User mechanic = verification.getMechanic();
        MechanicConfigurationSettings settings = mechanicConfigurationSettingsRepository.findByMechanicId(mechanic.getId()).orElse(null);
        String salesAgentName = referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .map(referral -> referral.getSalesRep().getFirstName() + " " + referral.getSalesRep().getLastName())
                .orElse(null);
        String decidedByAdminName = verification.getDecidedByAdminId() == null ? null
                : adminUserRepository.findById(verification.getDecidedByAdminId()).map(AdminUser::getFullName).orElse(null);

        return WorkshopVerificationDetailDto.builder()
                .id(verification.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .addressLine1(mechanic.getAddressLine1())
                .addressLine2(mechanic.getAddressLine2())
                .city(mechanic.getCity())
                .state(mechanic.getState())
                .postalCode(mechanic.getPostalCode())
                .country(mechanic.getCountry())
                .verificationType(verification.getVerificationType())
                .status(verification.getStatus())
                .requestedAt(verification.getRequestedAt())
                .registeredAt(mechanic.getCreatedAt())
                .accountStatus(accountStatus(mechanic))
                .rating(mechanic.getRating())
                .totalReviews(mechanic.getTotalReviews())
                .totalBookingsCompleted(mechanic.getTotalBookingsCompleted())
                .pickupDropEnabled(settings != null && Boolean.TRUE.equals(settings.getPickupDropFacilityEnabled()))
                .advancePaymentEnabled(settings != null && Boolean.TRUE.equals(settings.getRepairsRequireAdvance()))
                .salesAgentName(salesAgentName)
                .decisionReason(verification.getDecisionReason())
                .decidedByAdminName(decidedByAdminName)
                .decidedAt(verification.getDecidedAt())
                .build();
    }
}
