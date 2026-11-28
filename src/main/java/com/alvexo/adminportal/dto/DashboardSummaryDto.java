package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI counts for the Admin Dashboard landing page (ADMIN-US-01). Only the
 * counts this service currently has real data for — rider/workshop/sales-agent
 * registered vs. soft-deleted, workshop operational-control adoption,
 * suspended/deleted workshop and sales-agent counts, and (since ADMIN-US-06)
 * Listed/Platform Trusted counts from approved WorkshopVerification rows,
 * today's bookings (BR-14/15), settlement workshop counts (BR-16/17) and open
 * workshop support requests (BR-19). "New Feedback awaiting review" (BR-20)
 * has no data source yet and is not included.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {

    private long registeredRiders;
    private long deletedRiders;
    private long registeredWorkshops;
    private long deletedWorkshops;
    private long suspendedWorkshops;
    private long listedWorkshops;
    private long platformTrustedWorkshops;
    private long pickupDropEnabledWorkshops;
    private long advancePaymentEnabledWorkshops;
    private long registeredSalesAgents;
    private long suspendedSalesAgents;
    private long deletedSalesAgents;

    /** BR-14: bookings scheduled for today (India time), excluding cancelled and rejected ones. */
    private long bookingsScheduledToday;
    /** BR-15: bookings cancelled today. */
    private long bookingsCancelledToday;
    /** BR-16: workshops with an unpaid settlement due today or earlier (awaiting payout, processing, on hold, failed). */
    private long workshopsPendingSettlement;
    /** BR-17: workshops whose settlement was paid out today. */
    private long workshopsSettledToday;
    /** Finance summary: rider payments captured through Cashfree today (₹, advance + platform fee). */
    private java.math.BigDecimal paymentsToday;
    /** Finance summary: number of those payments. */
    private long paymentCountToday;
    /** Finance summary: refunds completed by Cashfree today (₹). */
    private java.math.BigDecimal refundsToday;
    /** BR-19: workshop support requests that are open or in progress. */
    private long openSupportRequests;
}
