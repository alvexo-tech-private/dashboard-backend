package com.alvexo.adminportal.report;

import com.alvexo.adminportal.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateRangeTest {

    @Test
    void endIsExclusiveSoTheWholeLastDayIsIncluded() {
        DateRange range = DateRange.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1));
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), range.start());
        assertEquals(LocalDateTime.of(2026, 11, 2, 0, 0), range.endExclusive());
    }

    @Test
    void rejectsReversedMissingOrOverlongRanges() {
        LocalDate day = LocalDate.of(2026, 11, 10);
        assertThrows(ApiException.class, () -> DateRange.of(day, day.minusDays(1)));
        assertThrows(ApiException.class, () -> DateRange.of(null, day));
        assertThrows(ApiException.class, () -> DateRange.of(day, day.plusDays(DateRange.MAX_DAYS)));
        DateRange.of(day, day.plusDays(DateRange.MAX_DAYS - 1));
    }
}
