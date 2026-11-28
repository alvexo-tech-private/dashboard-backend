package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only: a rider refund decided by the app's cancellation rules (time + location) and executed by Cashfree
 * (ADMIN-US-11 BR-25–27: no manual refunds from the portal).
 */
@Entity
@Table(name = "refunds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /** RIDER_CANCEL, WORKSHOP_CANCEL, FAILED_CONFIRMATION, DUPLICATE_PAYMENT. */
    @Column(nullable = false, length = 24)
    private String reason;

    @Column(name = "advance_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal advanceAmount;

    @Column(name = "platform_fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RefundStatus status;

    /** Cashfree refund id. */
    @Column(name = "provider_refund_id", length = 64)
    private String providerRefundId;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
