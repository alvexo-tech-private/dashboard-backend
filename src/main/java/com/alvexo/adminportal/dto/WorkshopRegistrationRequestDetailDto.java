package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.WorkshopRegistrationRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopRegistrationRequestDetailDto {

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
    private LocalDateTime registeredAt;
    private String referredSalesAgentName;
    private WorkshopRegistrationRequestStatus status;
    private String decisionReason;
    private String decidedByAdminName;
    private LocalDateTime decidedAt;
}
