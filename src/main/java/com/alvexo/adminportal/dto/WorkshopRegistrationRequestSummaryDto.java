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
public class WorkshopRegistrationRequestSummaryDto {

    private Long id;
    private String workshopName;
    private String ownerName;
    private String mobileNumber;
    private String city;
    private LocalDateTime registeredAt;
    private WorkshopRegistrationRequestStatus status;
}
