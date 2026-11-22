package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeAdminRoleRequestDto {

    @NotNull
    private AdminRole role;

    @NotBlank
    private String reason;
}
