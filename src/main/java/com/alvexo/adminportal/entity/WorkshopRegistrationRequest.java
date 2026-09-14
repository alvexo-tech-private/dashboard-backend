package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ADMIN-US-03 tracking row, one per workshop (mechanic) User — synced
 * lazily (see WorkshopRegistrationRequestService) rather than inserted by
 * the sibling app's registration flow, since that flow cannot write here.
 * `registeredAt` is copied from the mechanic's User.createdAt at sync time
 * and is distinct from this row's own `createdAt` (when the tracking row
 * itself was created).
 */
@Entity
@Table(name = "workshop_registration_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopRegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkshopRegistrationRequestStatus status;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "decided_by_admin_id")
    private Long decidedByAdminId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
