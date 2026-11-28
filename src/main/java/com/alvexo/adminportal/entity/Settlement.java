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
 * Maps to the existing `settlements` table, owned by vehicle-service-booking-api.
 * Since its changeset 072 this is one settlement per completed booking (not one
 * per workshop per day); mirrors com.alvexo.bookingapp.model.Settlement.
 */
@Entity
@Table(name = "settlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "service_completion_date")
    private LocalDate serviceCompletionDate;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "advance_collected", nullable = false, precision = 10, scale = 2)
    private BigDecimal advanceCollected;

    @Column(name = "advance_handling_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal advanceHandlingFee;

    @Column(name = "net_pay", nullable = false, precision = 10, scale = 2)
    private BigDecimal netPay;

    @Column(name = "tcs_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal tcsRate;

    @Column(name = "tcs_deduction", nullable = false, precision = 10, scale = 2)
    private BigDecimal tcsDeduction;

    @Column(name = "final_payout", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status;

    @Column(name = "expected_settlement_date")
    private LocalDate expectedSettlementDate;

    @Column(name = "payout_reference", unique = true, length = 24)
    private String payoutReference;

    @Column(name = "payout_date")
    private LocalDate payoutDate;

    @Column(name = "bank_reference", length = 40)
    private String bankReference;

    @Column(name = "provider_payout_id", unique = true, length = 64)
    private String providerPayoutId;

    @Column(name = "hold_reason")
    private String holdReason;

    /** The day-wise payout batch this settlement is paid in; null until its holding period ends. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private PayoutBatch batch;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
