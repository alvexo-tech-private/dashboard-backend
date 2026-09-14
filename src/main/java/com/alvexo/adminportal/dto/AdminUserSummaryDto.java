package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserSummaryDto {

    private Long id;
    private String email;
    private String fullName;
    private AdminRole role;
}
