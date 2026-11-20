package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One booking in a rider's or workshop's booking history (ADMIN-US-09 BR-04). Shows both parties so the same row works on either page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRowDto {

    private Long id;
    private String bookingNumber;
    private Long riderId;
    private String riderName;
    private Long mechanicId;
    private String workshopName;
    private String vehicle;
    private String serviceType;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime bookedAt;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}
