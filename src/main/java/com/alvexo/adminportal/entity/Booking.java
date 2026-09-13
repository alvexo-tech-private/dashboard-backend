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
 * Maps to the existing `bookings` table. `serviceSettingId`/`pickupMechanicId`/
 * `dropMechanicId` are plain FK columns rather than @ManyToOne relations
 * because their target tables (mechanic_service_settings, mechanic_master_entries)
 * are internal mechanic-scheduling detail this service deliberately doesn't
 * model — see dashboard/CLAUDE.md's entity-scope decision.
 */
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_user_id", nullable = false)
    private User vehicleUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false)
    private User mechanic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "service_setting_id")
    private Long serviceSettingId;

    @Column(name = "booking_number", nullable = false, unique = true)
    private String bookingNumber;

    @Column(name = "job_card_number", unique = true)
    private String jobCardNumber;

    @Column(name = "scheduled_date_time", nullable = false)
    private LocalDateTime scheduledDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_stage", length = 20)
    private ServiceDeskStage serviceStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private BookingChannel channel;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "advance_paid", precision = 10, scale = 2)
    private BigDecimal advancePaid;

    @Column(name = "mechanic_notes", columnDefinition = "TEXT")
    private String mechanicNotes;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancellation_message", columnDefinition = "TEXT")
    private String cancellationMessage;

    @Column(name = "reliability_adjustment_amount", precision = 10, scale = 2)
    private BigDecimal reliabilityAdjustmentAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "pickup_required", nullable = false)
    private Boolean pickupRequired;

    @Column(name = "drop_required", nullable = false)
    private Boolean dropRequired;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "pickup_mechanic_id")
    private Long pickupMechanicId;

    @Column(name = "drop_mechanic_id")
    private Long dropMechanicId;

    @Column(name = "delivered_by", length = 100)
    private String deliveredBy;

    @Column(name = "delivered_on")
    private LocalDate deliveredOn;

    @Column(name = "is_carry_over", nullable = false)
    private Boolean isCarryOver;

    @Column(name = "audio_reference", length = 255)
    private String audioReference;

    @Column(name = "engine_oil_replacement", nullable = false)
    private Boolean engineOilReplacement;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
