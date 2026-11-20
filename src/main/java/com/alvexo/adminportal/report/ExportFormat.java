package com.alvexo.adminportal.report;

import com.alvexo.adminportal.exception.ApiException;

import java.util.Locale;

/** File formats a report can be exported in (ADMIN-US-22 BR-15: CSV or XLSX). */
public enum ExportFormat {
    CSV("text/csv; charset=UTF-8", "csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String contentType;
    private final String extension;

    ExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static ExportFormat parse(String value) {
        try {
            return ExportFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw ApiException.badRequest("Export format must be CSV or XLSX.");
        }
    }
}
