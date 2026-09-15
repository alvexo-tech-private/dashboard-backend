package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ADMIN-US-06 tracking row — one per (workshop, verification type) pair,
 * synced lazily on read (see WorkshopVerificationService), same reasoning
 * as WorkshopRegistrationRequest: the sibling app's registration flow is
 * the only thing that creates mechanics, and it cannot write here.
 *
 * `requestedAt` is copied from the mechanic's User.createdAt at sync time —
 * there is no separate workshop-initiated "apply for Listed/Trust status"
 * flow to timestamp against, so registration order stands in for queue
 * order (BR-03: oldest pending request first).
 *
 * Listed/Trust "qualification rules" (BR-04/BR-07) are Epic 10 (Business
 * Rules & Commercial Configuration) territory and aren't built yet, so
 * approval here is a manual admin judgement call informed by the
 * qualification info surfaced in WorkshopVerificationDetailDto, not an
 * automated eligibility gate.
 */
@Entity
@Table(name = "workshop_verifications", uniqueConstraints = @UniqueConstraint(columnNames = {"mechanic_id", "verification_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    private WorkshopVerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkshopVerificationStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

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
