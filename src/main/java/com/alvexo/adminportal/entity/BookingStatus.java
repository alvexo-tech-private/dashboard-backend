package com.alvexo.adminportal.entity;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REJECTED,
    // Added to match vehicle-service-booking-api (two-stage / pay-to-confirm flows). Without them any
    // booking in these states fails to load here.
    REQUESTED,
    PENDING_PAYMENT,
    SCHEDULED,
    EXPIRED,
    AWAITING_PAYMENT
}
