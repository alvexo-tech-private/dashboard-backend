package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only: finance events written by vehicle-service-booking-api (batch created/extended, refund retry …). */
@Entity
@Table(name = "finance_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceAuditLog {

    @Id
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "entity_reference", nullable = false, length = 40)
    private String entityReference;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
