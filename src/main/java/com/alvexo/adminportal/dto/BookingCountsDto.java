package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Booking counts for one rider or workshop (ADMIN-US-08 BR-07 "Booking Summary").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCountsDto {

    private long total;
    private long upcoming;
    private long completed;
    private long cancelled;
}
