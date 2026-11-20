package com.alvexo.adminportal.report;

import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The one export path for every report (ADMIN-US-22): serializes the rows the
 * report service already built for the screen, and writes an audit_log entry
 * for the export itself (BR-19/20) — who, when, which report, range, filters,
 * format and row count. The export never touches the source records.
 */
@Service
@RequiredArgsConstructor
public class ReportExportService {

    /** Larger exports are refused with a request to narrow the range, rather than risking memory. */
    public static final int MAX_EXPORT_ROWS = 50_000;

    public static final String AUDIT_ENTITY_TYPE = "REPORT_EXPORT";
    public static final String AUDIT_ACTION = "EXPORT";

    private final AuditLogService auditLogService;

    /** Rows to fetch for an export: one past the limit, so {@link #export} can tell when it was exceeded. */
    public static int fetchLimit() {
        return MAX_EXPORT_ROWS + 1;
    }

    public <T> ResponseEntity<byte[]> export(ReportType type, ExportFormat format, DateRange range,
                                             Map<String, Object> filters, List<ReportColumn<T>> columns,
                                             List<T> rows, Long adminId) {
        if (rows.size() > MAX_EXPORT_ROWS) {
            throw ApiException.badRequest("This export has more than " + MAX_EXPORT_ROWS
                    + " rows. Pick a shorter date range or add a filter.");
        }
        byte[] file = ReportFileWriter.write(format, sheetName(type), columns, rows);

        auditLogService.record(AUDIT_ENTITY_TYPE, adminId, adminId, null, type.name(), AUDIT_ACTION,
                describe(format, range, filters, rows.size()));

        String filename = type.fileSlug() + "_" + range.from() + "_to_" + range.to() + "." + format.extension();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(format.contentType()))
                .body(file);
    }

    static String describe(ExportFormat format, DateRange range, Map<String, Object> filters, int rowCount) {
        String applied = filters.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
        return "Format: " + format.name()
                + "; Range: " + range
                + "; Filters: " + (applied.isEmpty() ? "none" : applied)
                + "; Rows: " + rowCount;
    }

    private static String sheetName(ReportType type) {
        String name = type.name().replace('_', ' ');
        return name.substring(0, 1) + name.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
