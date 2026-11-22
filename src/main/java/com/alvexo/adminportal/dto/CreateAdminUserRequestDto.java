package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAdminUserRequestDto {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotNull
    private AdminRole role;

    /** Optional note kept in the audit log (e.g. "New finance team member"). */
    @Size(max = 1000)
    private String reason;
}
