package com.alvexo.adminportal.entity;

/**
 * Admin-facing status of a rider / mechanic / sales-agent account, derived
 * from the booking API's existing `users.active` and `users.deleted`
 * booleans — there is deliberately no separate status column:
 * deleted = true -> DELETED; active = false -> SUSPENDED; otherwise ACTIVE.
 *
 * The portal's soft delete sets BOTH deleted = true and active = false, so the
 * booking API's existing `active` checks (login, mechanic search) apply to
 * deleted accounts without that API having to know about `deleted`.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETED;

    public static AccountStatus of(User user) {
        if (Boolean.TRUE.equals(user.getDeleted())) return DELETED;
        if (!Boolean.TRUE.equals(user.getActive())) return SUSPENDED;
        return ACTIVE;
    }
}
