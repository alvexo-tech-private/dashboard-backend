package com.alvexo.adminportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Reused for both Reject (mandatory reason) and Request-Additional-Information (mandatory comments). */
@Data
public class DecisionRequestDto {

    @NotBlank
    private String reason;
}
