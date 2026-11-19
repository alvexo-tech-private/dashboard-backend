package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A workshop the rider has booked with, derived from their bookings (ADMIN-US-09 BR-07).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderWorkshopDto {

    private Long mechanicId;
    private String workshopName;
    private long bookings;
    private LocalDateTime lastBookingAt;
}
