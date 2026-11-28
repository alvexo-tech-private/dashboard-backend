package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A workshop's consolidated settlement for one day (WPB-YYYYMMDD-WS003), created by vehicle-service-booking-api
 * once the bookings' holding period is over. The portal only reads it, except the Super Admin offline closure.
 * updated_by_id belongs to the app's users table, so it is not mapped here (admin actions go to audit_log).
 */
@Entity
@Table(name = "payout_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_reference", nullable = false, unique = true, length = 32)
    private String batchReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutBatchStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "booking_count", nullable = false)
    private Integer bookingCount;

    @Column(name = "payment_mode", length = 30)
    private String paymentMode;

    /** Bank UTR / gateway settlement reference. */
    @Column(length = 100)
    private String utr;

    @Column(length = 500)
    private String note;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
