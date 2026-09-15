package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ADMIN-US-02 Workshop Summary (BR-06..BR-11). Business Information (BR-08:
 * supported vehicle types, working days/hours) is omitted rather than faked —
 * it lives on mechanic-scheduling tables (MechanicServiceSetting, MechanicAvailability)
 * this service deliberately doesn't map, see dashboard/CLAUDE.md's entity-scope
 * decision. Listed/Platform Trusted status (part of BR-09) is omitted for the
 * same reason as MechanicSummaryDto — see its Javadoc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkshopSummaryDto {

    // Basic Information (BR-07)
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

    // Platform Information (BR-09)
    private LocalDateTime registeredAt;
    private Boolean pickupDropEnabled;
    private Boolean advancePaymentEnabled;
    private String salesAgentName;
    private String accountStatus;

    // Booking Summary (BR-10)
    private long totalBookings;
    private long upcomingBookings;
    private long cancelledBookings;

    // Finance Summary (BR-11)
    private LocalDate lastSettlementDate;
    private SettlementStatus lastSettlementStatus;
}
