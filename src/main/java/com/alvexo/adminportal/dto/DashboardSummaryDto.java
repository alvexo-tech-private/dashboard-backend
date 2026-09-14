package com.alvexo.adminportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI counts for the Admin Dashboard landing page (ADMIN-US-01). Only the
 * counts this service currently has real data for — rider/workshop/sales-agent
 * registered vs. soft-deleted, workshop operational-control adoption,
 * and suspended/deleted workshop and sales-agent counts — are populated;
 * other dashboard tiles (bookings, finance, support) stay placeholders
 * until their epics are built.
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
    private long pickupDropEnabledWorkshops;
    private long advancePaymentEnabledWorkshops;
    private long registeredSalesAgents;
    private long suspendedSalesAgents;
    private long deletedSalesAgents;
}
