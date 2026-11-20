package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One vehicle registered by a rider (ADMIN-US-09 BR-03).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderVehicleDto {

    private Long id;
    private String make;
    private String model;
    private Integer year;
    private String vehicleType;
    private String registrationNumber;
    private Boolean primary;
    private LocalDateTime addedAt;
}
