package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sales agent profile with referral totals (ADMIN-US-10 BR-09/10). Read-only; KYC approval is a later story.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesAgentDetailDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String city;
    private String state;
    private String referralCode;
    private LocalDateTime registeredAt;
    private String accountStatus;
    private LocalDateTime deletedAt;
    private long referredWorkshops;
    private long activeReferredWorkshops;
    private long referredOthers;
    private BigDecimal bonusPaid;
    private BigDecimal bonusPending;
}
