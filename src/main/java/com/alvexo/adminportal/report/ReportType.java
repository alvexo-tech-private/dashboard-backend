package com.alvexo.adminportal.report;

/** Every exportable report; the name is what the audit log records as the report type (ADMIN-US-22 BR-20). */
public enum ReportType {
    BOOKINGS("bookings"),
    REGISTRATIONS("registrations"),
    WORKSHOP_PERFORMANCE("workshop-performance"),
    SETTLEMENTS_BY_WORKSHOP("settlements-by-workshop"),
    SETTLEMENT_LINES("settlement-lines");

    private final String fileSlug;

    ReportType(String fileSlug) {
        this.fileSlug = fileSlug;
    }

    public String fileSlug() {
        return fileSlug;
    }
}
