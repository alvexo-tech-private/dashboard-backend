package com.alvexo.adminportal.report;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFileWriterTest {

    record Line(String name, BigDecimal amount, LocalDate date) {
    }

    private static final List<ReportColumn<Line>> COLUMNS = List.of(
            ReportColumn.of("Name", Line::name),
            ReportColumn.of("Amount (INR)", Line::amount),
            ReportColumn.of("Date", Line::date));

    @Test
    void csvQuotesSpecialCharactersAndNeutralisesFormulas() {
        byte[] file = ReportFileWriter.write(ExportFormat.CSV, "Test", COLUMNS, List.of(
                new Line("Rao, \"Motors\"", new BigDecimal("1250.50"), LocalDate.of(2026, 11, 3)),
                new Line("=HYPERLINK(\"x\")", new BigDecimal("-20.00"), null)));

        String csv = new String(file, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"), "UTF-8 BOM so Excel reads the encoding");
        String[] lines = csv.substring(1).split("\r\n");
        assertEquals("Name,Amount (INR),Date", lines[0]);
        assertEquals("\"Rao, \"\"Motors\"\"\",1250.50,2026-11-03", lines[1]);
        // Text starting with '=' is prefixed so it can't run as a formula; negative numbers stay numbers.
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\",-20.00,", lines[2]);
    }

    @Test
    void emptyReportStillHasTheHeaderRow() {
        String csv = new String(ReportFileWriter.write(ExportFormat.CSV, "Test", COLUMNS, List.of()), StandardCharsets.UTF_8);
        assertEquals("\uFEFFName,Amount (INR),Date\r\n", csv);
    }

    @Test
    void xlsxWritesNumbersAsNumericCells() throws Exception {
        byte[] file = ReportFileWriter.write(ExportFormat.XLSX, "Bookings", COLUMNS,
                List.of(new Line("Rao Motors", new BigDecimal("99.95"), LocalDate.of(2026, 1, 2))));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            Sheet sheet = workbook.getSheet("Bookings");
            assertEquals("Amount (INR)", sheet.getRow(0).getCell(1).getStringCellValue());
            Row row = sheet.getRow(1);
            assertEquals("Rao Motors", row.getCell(0).getStringCellValue());
            assertEquals(99.95, row.getCell(1).getNumericCellValue(), 1e-9);
            assertEquals("2026-01-02", row.getCell(2).getStringCellValue());
        }
    }
}
