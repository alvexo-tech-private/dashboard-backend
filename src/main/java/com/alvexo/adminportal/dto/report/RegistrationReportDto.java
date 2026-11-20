package com.alvexo.adminportal.dto.report;

import java.util.List;

/** New registrations report: one row per day in the range (zero days included) and the range totals. */
public record RegistrationReportDto(RegistrationReportRowDto totals, List<RegistrationReportRowDto> days) {
}
