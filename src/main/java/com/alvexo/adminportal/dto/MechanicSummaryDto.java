package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Directory-level summary of a `users` row with role = MECHANIC — enough for
 * a mechanic/workshop listing screen, not a full profile dump.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicSummaryDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String workshopName;
    private String city;
    private String state;
    private String specialization;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal rating;
    private Boolean active;
}
