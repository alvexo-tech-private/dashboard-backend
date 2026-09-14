package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Directory-level summary of a `users` row with role = SALES_REPRESENTATIVE
 * — enough for a sales agent listing screen, not a full profile dump.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesAgentSummaryDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String city;
    private String referralCode;
    private Integer totalReferrals;
    private BigDecimal totalBonusEarned;
    private Boolean active;
    private LocalDateTime createdAt;
}
