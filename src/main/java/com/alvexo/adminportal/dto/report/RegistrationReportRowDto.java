package com.alvexo.adminportal.dto.report;

import java.time.LocalDate;

/** New accounts created on one day, by account type. */
public record RegistrationReportRowDto(LocalDate date, long riders, long workshops, long salesAgents, long total) {
}
