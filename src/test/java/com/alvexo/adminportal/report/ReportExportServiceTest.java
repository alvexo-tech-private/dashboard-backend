package com.alvexo.adminportal.report;

import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReportExportServiceTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final ReportExportService service = new ReportExportService(auditLogService);
    private final DateRange range = DateRange.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20));
    private final List<ReportColumn<String>> columns = List.of(ReportColumn.of("Value", v -> v));

    @Test
    void exportIsAuditedWithAdminRangeFiltersAndRowCount() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("status", "COMPLETED");
        filters.put("workshopId", null);

        ResponseEntity<byte[]> response = service.export(ReportType.BOOKINGS, ExportFormat.XLSX, range, filters,
                columns, List.of("a", "b"), 7L);

        verify(auditLogService).record("REPORT_EXPORT", 7L, 7L, null, "BOOKINGS", "EXPORT",
                "Format: XLSX; Range: 2026-11-01 to 2026-11-20; Filters: status=COMPLETED; Rows: 2");
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(disposition.contains("bookings_2026-11-01_to_2026-11-20.xlsx"), disposition);
    }

    @Test
    void tooManyRowsIsRefusedAndNotAudited() {
        List<String> rows = Collections.nCopies(ReportExportService.MAX_EXPORT_ROWS + 1, "x");

        assertThrows(ApiException.class, () -> service.export(ReportType.BOOKINGS, ExportFormat.CSV, range,
                Map.of(), columns, rows, 7L));
        verify(auditLogService, never()).record(anyString(), anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void formatIsCaseInsensitiveAndValidated() {
        assertEquals(ExportFormat.CSV, ExportFormat.parse("csv"));
        assertThrows(ApiException.class, () -> ExportFormat.parse("pdf"));
    }
}
