package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One rider review of a workshop booking (ADMIN-US-09 BR-10 feedback). Read-only.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRowDto {

    private Long id;
    private String bookingNumber;
    private Long riderId;
    private String riderName;
    private Long mechanicId;
    private String workshopName;
    private Integer rating;
    private String comment;
    private String workshopResponse;
    private LocalDateTime createdAt;
}
