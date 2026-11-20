package com.alvexo.adminportal.entity;

/** Mirrors vehicle-service-booking-api's WorkshopStatus (workshop_profiles.status). */
public enum WorkshopStatus {
    IN_PROGRESS,
    PENDING,
    REJECTED,
    NEW_TO_APP,
    VERIFIED,
    SUSPENDED
}
