package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Mirror of vehicle-service-booking-api's `workshop_profiles` (owned and
 * migrated by that service). `status` is what the workshop app's Current
 * Status card and the rider booking gate read: VERIFIED = Self Listed.
 */
@Entity
@Table(name = "workshop_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkshopStatus status;

    @Column(name = "suspended", nullable = false)
    private Boolean suspended;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
