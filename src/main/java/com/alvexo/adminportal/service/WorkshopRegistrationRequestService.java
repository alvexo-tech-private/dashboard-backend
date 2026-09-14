package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.WorkshopRegistrationRequestDetailDto;
import com.alvexo.adminportal.dto.WorkshopRegistrationRequestSummaryDto;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.Notification;
import com.alvexo.adminportal.entity.NotificationType;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.entity.WorkshopRegistrationRequest;
import com.alvexo.adminportal.entity.WorkshopRegistrationRequestStatus;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.NotificationRepository;
import com.alvexo.adminportal.repository.ReferralRepository;
import com.alvexo.adminportal.repository.UserRepository;
import com.alvexo.adminportal.repository.WorkshopRegistrationRequestRepository;
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
 * ADMIN-US-03 — Manage Workshop Registration Requests.
 *
 * Approval semantics (confirmed with the user): a mechanic's `users` row is
 * created immediately active by the sibling app's registration flow, so
 * there is no real "pending, not yet usable" account state to gate.
 * Approve is therefore record-only; Reject is the one real enforcement
 * lever, flipping the mechanic's User.active to false.
 *
 * Since mechanic registration happens entirely inside the sibling repo
 * (which this service cannot write to for its own table), nothing ever
 * inserts a row into workshop_registration_requests on its own — this
 * service lazily syncs on every read instead.
 */
@Service
@RequiredArgsConstructor
public class WorkshopRegistrationRequestService {

    private static final String ENTITY_TYPE = "WORKSHOP_REGISTRATION_REQUEST";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_FIELDS = Set.of("registeredAt", "status", "id");
    private static final Set<WorkshopRegistrationRequestStatus> TERMINAL_STATUSES =
            Set.of(WorkshopRegistrationRequestStatus.APPROVED, WorkshopRegistrationRequestStatus.REJECTED);

    private final WorkshopRegistrationRequestRepository workshopRegistrationRequestRepository;
    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;
    private final AdminUserRepository adminUserRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PagedResponse<WorkshopRegistrationRequestSummaryDto> getQueue(int page, int size, String search, String sortBy, String sortDir) {
        syncNewRequests();
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<WorkshopRegistrationRequest> result = workshopRegistrationRequestRepository.searchQueue(normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    @Transactional
    public PagedResponse<WorkshopRegistrationRequestSummaryDto> getAll(int page, int size, String search, String sortBy, String sortDir) {
        syncNewRequests();
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<WorkshopRegistrationRequest> result = workshopRegistrationRequestRepository.searchAll(normalize(search), pageable);
        return PagedResponse.from(result, this::toSummaryDto);
    }

    public WorkshopRegistrationRequestDetailDto getById(Long id) {
        return toDetailDto(getOrThrow(id));
    }

    @Transactional
    public void approve(Long id, Long adminId) {
        WorkshopRegistrationRequest request = getOrThrow(id);
        guardNotTerminal(request);
        WorkshopRegistrationRequestStatus previous = request.getStatus();

        request.setStatus(WorkshopRegistrationRequestStatus.APPROVED);
        request.setDecidedByAdminId(adminId);
        request.setDecidedAt(LocalDateTime.now());
        workshopRegistrationRequestRepository.save(request);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), request.getStatus().name(), "APPROVE", null);
        notifyDecision(request, NotificationType.WORKSHOP_REGISTRATION_APPROVED, "Your workshop registration has been approved.");
    }

    @Transactional
    public void reject(Long id, Long adminId, String reason) {
        WorkshopRegistrationRequest request = getOrThrow(id);
        guardNotTerminal(request);
        WorkshopRegistrationRequestStatus previous = request.getStatus();

        request.setStatus(WorkshopRegistrationRequestStatus.REJECTED);
        request.setDecisionReason(reason);
        request.setDecidedByAdminId(adminId);
        request.setDecidedAt(LocalDateTime.now());
        workshopRegistrationRequestRepository.save(request);

        // The one real enforcement lever available today — see class Javadoc.
        User mechanic = request.getMechanic();
        mechanic.setActive(false);
        userRepository.save(mechanic);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), request.getStatus().name(), "REJECT", reason);
        notifyDecision(request, NotificationType.WORKSHOP_REGISTRATION_REJECTED,
                "Your workshop registration has been rejected. Reason: " + reason);
    }

    @Transactional
    public void requestInfo(Long id, Long adminId, String comments) {
        WorkshopRegistrationRequest request = getOrThrow(id);
        guardNotTerminal(request);
        WorkshopRegistrationRequestStatus previous = request.getStatus();

        request.setStatus(WorkshopRegistrationRequestStatus.ADDITIONAL_INFO_REQUESTED);
        request.setDecisionReason(comments);
        request.setDecidedByAdminId(adminId);
        request.setDecidedAt(LocalDateTime.now());
        workshopRegistrationRequestRepository.save(request);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), request.getStatus().name(), "REQUEST_INFO", comments);
        notifyDecision(request, NotificationType.WORKSHOP_REGISTRATION_INFO_REQUESTED,
                "Additional information is required for your workshop registration: " + comments);
    }

    /**
     * Interim workaround: there is currently no mechanic-facing resubmission
     * endpoint anywhere (that would need to live in the sibling repo), so an
     * Operations Admin manually confirms information was received offline
     * and moves the request back to Pending for re-review.
     */
    @Transactional
    public void markResubmitted(Long id, Long adminId) {
        WorkshopRegistrationRequest request = getOrThrow(id);
        if (request.getStatus() != WorkshopRegistrationRequestStatus.ADDITIONAL_INFO_REQUESTED) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "Only requests currently Additional Information Requested can be marked resubmitted.");
        }
        WorkshopRegistrationRequestStatus previous = request.getStatus();
        request.setStatus(WorkshopRegistrationRequestStatus.PENDING);
        workshopRegistrationRequestRepository.save(request);

        auditLogService.record(ENTITY_TYPE, id, adminId, previous.name(), request.getStatus().name(), "MARK_RESUBMITTED", null);
    }

    private void syncNewRequests() {
        List<User> mechanics = userRepository.findByRoleAndDeletedFalse(UserRole.MECHANIC);
        Set<Long> existingMechanicIds = workshopRegistrationRequestRepository.findAllMechanicIds();
        for (User mechanic : mechanics) {
            if (!existingMechanicIds.contains(mechanic.getId())) {
                workshopRegistrationRequestRepository.save(WorkshopRegistrationRequest.builder()
                        .mechanic(mechanic)
                        .status(WorkshopRegistrationRequestStatus.PENDING)
                        .registeredAt(mechanic.getCreatedAt())
                        .build());
            }
        }
    }

    private void guardNotTerminal(WorkshopRegistrationRequest request) {
        if (TERMINAL_STATUSES.contains(request.getStatus())) {
            throw ApiException.conflict("ALREADY_DECIDED", "This registration request has already been decided.");
        }
    }

    private void notifyDecision(WorkshopRegistrationRequest request, NotificationType type, String message) {
        User mechanic = request.getMechanic();
        notificationRepository.save(Notification.builder()
                .user(mechanic)
                .title("Workshop Registration Update")
                .message(message)
                .notificationType(type)
                .isRead(false)
                .relatedEntityType(ENTITY_TYPE)
                .relatedEntityId(request.getId())
                .build());

        referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .ifPresent(referral -> notificationRepository.save(Notification.builder()
                        .user(referral.getSalesRep())
                        .title("Referred Workshop Registration Update")
                        .message("Workshop " + mechanic.getWorkshopName() + ": " + message)
                        .notificationType(type)
                        .isRead(false)
                        .relatedEntityType(ENTITY_TYPE)
                        .relatedEntityId(request.getId())
                        .build()));
    }

    private WorkshopRegistrationRequest getOrThrow(Long id) {
        return workshopRegistrationRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Registration request not found."));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String sortField = SORT_FIELDS.contains(sortBy) ? sortBy : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));
    }

    private String normalize(String search) {
        return search == null ? "" : search.trim();
    }

    private WorkshopRegistrationRequestSummaryDto toSummaryDto(WorkshopRegistrationRequest request) {
        User mechanic = request.getMechanic();
        return WorkshopRegistrationRequestSummaryDto.builder()
                .id(request.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .city(mechanic.getCity())
                .registeredAt(request.getRegisteredAt())
                .status(request.getStatus())
                .build();
    }

    private WorkshopRegistrationRequestDetailDto toDetailDto(WorkshopRegistrationRequest request) {
        User mechanic = request.getMechanic();
        String referredSalesAgentName = referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(mechanic.getId())
                .map(referral -> referral.getSalesRep().getFirstName() + " " + referral.getSalesRep().getLastName())
                .orElse(null);
        String decidedByAdminName = request.getDecidedByAdminId() == null ? null
                : adminUserRepository.findById(request.getDecidedByAdminId()).map(AdminUser::getFullName).orElse(null);

        return WorkshopRegistrationRequestDetailDto.builder()
                .id(request.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(mechanic.getFirstName() + " " + mechanic.getLastName())
                .mobileNumber(mechanic.getMobileNumber())
                .addressLine1(mechanic.getAddressLine1())
                .addressLine2(mechanic.getAddressLine2())
                .city(mechanic.getCity())
                .state(mechanic.getState())
                .postalCode(mechanic.getPostalCode())
                .country(mechanic.getCountry())
                .registeredAt(request.getRegisteredAt())
                .referredSalesAgentName(referredSalesAgentName)
                .status(request.getStatus())
                .decisionReason(request.getDecisionReason())
                .decidedByAdminName(decidedByAdminName)
                .decidedAt(request.getDecidedAt())
                .build();
    }
}
