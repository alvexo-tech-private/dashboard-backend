package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Mirror of vehicle-service-booking-api's `workshop_verifications` — the
 * Platform Verified (Trust) request, one row per workshop. The workshop app
 * submits `completedMethods` with `verified = false`; an admin approving it
 * in the ADMIN-US-06 queue is what sets `verified = true`.
 */
@Entity
@Table(name = "workshop_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id", nullable = false, unique = true)
    private User mechanic;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workshop_verification_methods", joinColumns = @JoinColumn(name = "workshop_verification_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 30)
    @Builder.Default
    private Set<VerificationMethod> completedMethods = new HashSet<>();

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
