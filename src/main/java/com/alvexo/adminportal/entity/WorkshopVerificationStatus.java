package com.alvexo.adminportal.entity;

public enum WorkshopVerificationStatus {
    /** The workshop hasn't submitted Platform Verified methods from the app yet. */
    NOT_SUBMITTED,
    PENDING,
    ADDITIONAL_INFO_REQUESTED,
    APPROVED,
    REJECTED
}
