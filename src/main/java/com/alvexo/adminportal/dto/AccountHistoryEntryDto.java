package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** One audited suspend / restore / delete action on a user account. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHistoryEntryDto {

    private Long id;
    private String action;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private String performedBy;
    private LocalDateTime performedAt;
}
