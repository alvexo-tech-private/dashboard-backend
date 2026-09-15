package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.WorkshopVerificationStatus;
import com.alvexo.adminportal.entity.WorkshopVerificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ADMIN-US-06 verification detail — AC-03 "selecting a request displays the
 * required workshop qualification information". Since the actual Listed/Trust
 * qualification rules (Epic 10) aren't configured anywhere yet, this surfaces
 * whatever operational signal already exists (rating, completed bookings,
 * existing operational-control adoption, account standing) for the admin to
 * judge manually, rather than an automated pass/fail against a rule set that
 * doesn't exist.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopVerificationDetailDto {

    private Long id;
    private String workshopName;
    private String ownerName;
    private String mobileNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private WorkshopVerificationType verificationType;
    private WorkshopVerificationStatus status;
    private LocalDateTime requestedAt;

    // Qualification information (BR-05/BR-08) available today.
    private LocalDateTime registeredAt;
    private String accountStatus;
    private BigDecimal rating;
    private Integer totalReviews;
    private Integer totalBookingsCompleted;
    private Boolean pickupDropEnabled;
    private Boolean advancePaymentEnabled;
    private String salesAgentName;

    private String decisionReason;
    private String decidedByAdminName;
    private LocalDateTime decidedAt;
}
