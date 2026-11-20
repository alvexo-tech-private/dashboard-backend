package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Read-only view of the booking API's {@code workshop_support_requests} table
 * (help requests a workshop raises from the app). Only the columns the
 * dashboard counts are mapped; the portal never writes to this table.
 */
@Entity
@Immutable
@Table(name = "workshop_support_requests")
@Getter
@NoArgsConstructor
public class WorkshopSupportRequest {

    @Id
    private Long id;

    @Column(name = "mechanic_id", nullable = false)
    private Long mechanicId;

    @Column(name = "reference_id", nullable = false, length = 30)
    private String referenceId;

    @Column(name = "subject", nullable = false, length = 150)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupportRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
