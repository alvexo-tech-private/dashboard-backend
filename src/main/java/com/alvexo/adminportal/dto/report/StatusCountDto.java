package com.alvexo.adminportal.dto.report;

/** How many report rows have one status (ADMIN-US-20 BR-09: counts grouped by status). */
public record StatusCountDto(String status, long count) {
}
