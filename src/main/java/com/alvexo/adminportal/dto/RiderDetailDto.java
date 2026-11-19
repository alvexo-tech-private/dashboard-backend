package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ADMIN-US-08 BR-07 Rider Details: profile, vehicles, booking summary, workshops used, status. Read-only.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderDetailDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private Boolean emailVerified;
    private Boolean mobileVerified;
    private String addressLine1;
    private String addressLine2;
    private String area;
    private String city;
    private String state;
    private String postalCode;
    private LocalDateTime registeredAt;
    private String accountStatus;
    private LocalDateTime deletedAt;
    private BookingCountsDto bookings;
    private List<RiderVehicleDto> vehicles;
    private List<RiderWorkshopDto> workshops;
}
