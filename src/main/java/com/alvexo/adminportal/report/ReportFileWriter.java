package com.alvexo.adminportal.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serializes report rows to CSV or XLSX. Values are written as they are — no
 * rounding or recalculation (ADMIN-US-22 BR-13) — and an empty result still
 * produces a file with just the header row (AC-15).
 */
public final class ReportFileWriter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** Keeps only this many rows in memory while writing XLSX; older rows are flushed to a temp file. */
    private static final int XLSX_WINDOW = 200;

    private ReportFileWriter() {
    }

    public static <T> byte[] write(ExportFormat format, String sheetName, List<ReportColumn<T>> columns, List<T> rows) {
        return format == ExportFormat.CSV ? csv(columns, rows) : xlsx(sheetName, columns, rows);
    }

    static <T> byte[] csv(List<ReportColumn<T>> columns, List<T> rows) {
        StringBuilder out = new StringBuilder();
        // UTF-8 byte order mark so Excel opens the file with the right encoding (₹, non-Latin names).
        out.append('﻿');
        appendCsvLine(out, columns.stream().map(c -> (Object) c.header()).toList());
        for (T row : rows) {
            appendCsvLine(out, columns.stream().map(c -> c.value().apply(row)).toList());
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendCsvLine(StringBuilder out, List<Object> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(csvCell(values.get(i)));
        }
        out.append("\r\n");
    }

    static String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number) {
            return value instanceof java.math.BigDecimal bd ? bd.toPlainString() : value.toString();
        }
        String text = text(value);
        // A text cell starting with = + - @ would run as a formula when opened in Excel (CSV injection).
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static <T> byte[] xlsx(String sheetName, List<ReportColumn<T>> columns, List<T> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(XLSX_WINDOW);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(columns.get(c).header());
                cell.setCellStyle(headerStyle);
                // Column widths are set from the header, since SXSSF can't auto-size flushed rows.
                sheet.setColumnWidth(c, Math.min(60, Math.max(12, columns.get(c).header().length() + 4)) * 256);
            }
            sheet.createFreezePane(0, 1);

            int r = 1;
            for (T row : rows) {
                Row sheetRow = sheet.createRow(r++);
                for (int c = 0; c < columns.size(); c++) {
                    Object value = columns.get(c).value().apply(row);
                    if (value == null) {
                        continue;
                    }
                    Cell cell = sheetRow.createCell(c);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(text(value));
                    }
                }
            }
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the XLSX file", e);
        }
    }

    private static String text(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME.format(dateTime);
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof Boolean b) {
            return b ? "Yes" : "No";
        }
        return value.toString();
    }
}
