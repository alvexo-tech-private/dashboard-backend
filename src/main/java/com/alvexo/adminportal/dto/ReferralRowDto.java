package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One account a sales agent referred (ADMIN-US-10 BR-09): usually a workshop. Commission is not shown here (Epic 7).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralRowDto {

    private Long id;
    private Long referredUserId;
    private String referredName;
    private String workshopName;
    private String referredRole;
    private String referredAccountStatus;
    private String status;
    private BigDecimal bonusAmount;
    private Boolean bonusPaid;
    private LocalDateTime referredAt;
}
