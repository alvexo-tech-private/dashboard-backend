package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned once, when a Super Admin creates an admin or resets their
 * password. The temporary password is never stored in plain text and can't
 * be fetched again; the admin must replace it at first sign-in.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporaryPasswordDto {

    private AdminUserDto adminUser;
    private String temporaryPassword;
}
