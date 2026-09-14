package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Maps to vehicle-service-booking-api's existing `mechanic_configuration_settings`
 * table — the "Configuration Settings" section of Workshop Settings (job-card
 * numbering, reschedule policy, pickup/drop, advance-payment requirement,
 * service reminders). One row per mechanic, created on first save — a
 * workshop that has never touched settings has no row here (equivalent to
 * every toggle being at its default, e.g. pickup/drop and advance payment
 * both off).
 */
@Entity
@Table(name = "mechanic_configuration_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicConfigurationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Column(name = "job_card_number_starting_sequence", nullable = false, length = 20)
    private String jobCardNumberStartingSequence;

    @Column(name = "job_card_number_format", nullable = false, length = 20)
    private String jobCardNumberFormat;

    @Column(name = "job_card_number_reset_frequency", nullable = false, length = 20)
    private String jobCardNumberResetFrequency;

    @Column(name = "job_card_number_prefix", length = 4)
    private String jobCardNumberPrefix;

    @Column(name = "job_card_number_suffix", length = 4)
    private String jobCardNumberSuffix;

    @Column(name = "reschedule_limit", nullable = false)
    private Integer rescheduleLimit;

    @Column(name = "reschedule_cutoff_time", nullable = false)
    private LocalTime rescheduleCutoffTime;

    @Column(name = "pickup_drop_facility_enabled", nullable = false)
    private Boolean pickupDropFacilityEnabled;

    @Column(name = "auto_confirm_other_state", nullable = false)
    private Boolean autoConfirmOtherState;

    @Column(name = "repairs_require_advance", nullable = false)
    private Boolean repairsRequireAdvance;

    @Column(name = "service_due_interval_days", nullable = false)
    private Integer serviceDueIntervalDays;

    @Column(name = "second_reminder_interval_days", nullable = false)
    private Integer secondReminderIntervalDays;

    @Column(name = "reminder_changes_this_year", nullable = false)
    private Integer reminderChangesThisYear;

    @Column(name = "reminder_changes_year")
    private Integer reminderChangesYear;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
