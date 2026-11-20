package com.alvexo.adminportal.report;

import com.alvexo.adminportal.exception.ApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * An inclusive [from, to] calendar-date range picked on a report screen.
 * Timestamp columns are compared as {@code >= start() AND < endExclusive()},
 * so the whole of the {@code to} day is included.
 */
public record DateRange(LocalDate from, LocalDate to) {

    /** Keeps one request (and one export file) to a size the database and browser handle comfortably. */
    public static final int MAX_DAYS = 366;

    public static DateRange of(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ApiException.badRequest("Both a start date and an end date are required.");
        }
        if (to.isBefore(from)) {
            throw ApiException.badRequest("The end date must be on or after the start date.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw ApiException.badRequest("Pick a range of " + MAX_DAYS + " days or less.");
        }
        return new DateRange(from, to);
    }

    public LocalDateTime start() {
        return from.atStartOfDay();
    }

    public LocalDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay();
    }

    @Override
    public String toString() {
        return from + " to " + to;
    }
}
