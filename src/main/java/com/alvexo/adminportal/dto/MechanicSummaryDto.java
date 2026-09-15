package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Directory-level summary of a `users` row with role = MECHANIC — the
 * ADMIN-US-02 / BR-05 Workshop Directory row. `listed`/`platformTrusted`
 * are deliberately not included: they depend on the workshop_verification_status
 * table ADMIN-US-06 introduces, which doesn't exist yet (see dashboard/CLAUDE.md).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicSummaryDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String workshopName;
    private String city;
    private String state;
    private String specialization;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal rating;
    private LocalDateTime registeredAt;
    private Boolean pickupDropEnabled;
    private Boolean advancePaymentEnabled;
    /** ACTIVE, SUSPENDED or DELETED — derived from User.active/deleted. */
    private String accountStatus;
    private String salesAgentName;
}
