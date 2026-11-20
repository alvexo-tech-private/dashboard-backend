package com.alvexo.adminportal.report;

import java.util.function.Function;

/**
 * One column of an exported report: its header and how to read the cell from
 * a row DTO. Exports are built from the same row DTOs the screen shows, so the
 * file always matches the on-screen data (ADMIN-US-20 BR-17, US-22 BR-12).
 */
public record ReportColumn<T>(String header, Function<T, Object> value) {

    public static <T> ReportColumn<T> of(String header, Function<T, Object> value) {
        return new ReportColumn<>(header, value);
    }
}
