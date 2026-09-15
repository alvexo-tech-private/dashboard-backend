package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.WorkshopVerificationStatus;
import com.alvexo.adminportal.entity.WorkshopVerificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopVerificationSummaryDto {

    private Long id;
    private String workshopName;
    private String ownerName;
    private String mobileNumber;
    private String city;
    private WorkshopVerificationType verificationType;
    private WorkshopVerificationStatus status;
    private LocalDateTime requestedAt;
}
