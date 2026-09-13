package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Directory-level summary of a `users` row with role = VEHICLE_USER (rider) —
 * enough for a rider listing screen, not a full profile dump.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleUserSummaryDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String city;
    private String state;
    private Boolean emailVerified;
    private Boolean mobileVerified;
    private Boolean active;
    private LocalDateTime createdAt;
}
