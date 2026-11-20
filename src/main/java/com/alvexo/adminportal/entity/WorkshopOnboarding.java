package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirror of vehicle-service-booking-api's `workshop_onboardings` — one row
 * per workshop, created by the workshop app. A submitted row is what
 * ADMIN-US-03 calls a "registration request"; the admin's decision is
 * written here and onto the matching WorkshopProfile.status, exactly as the
 * booking API's own WorkshopProfileService.recordOnboardingDecision does.
 */
@Entity
@Table(name = "workshop_onboardings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "fee_paid", nullable = false)
    private Boolean feePaid;

    @Column(name = "payment_ref", length = 100)
    private String paymentRef;

    @Column(name = "reference_id", unique = true, length = 30)
    private String referenceId;

    @Column(name = "submitted", nullable = false)
    private Boolean submitted;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 20)
    private OnboardingDecision decision;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
