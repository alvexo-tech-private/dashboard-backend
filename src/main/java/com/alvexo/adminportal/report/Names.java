package com.alvexo.adminportal.report;

import com.alvexo.adminportal.entity.User;

/** Display names used in report rows. */
public final class Names {

    private Names() {
    }

    public static String person(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        return (first + " " + last).trim();
    }

    /** The workshop name a mechanic registered with, falling back to the owner's name. */
    public static String workshop(User mechanic) {
        if (mechanic == null) {
            return null;
        }
        String name = mechanic.getWorkshopName();
        return name != null && !name.isBlank() ? name.trim() : person(mechanic);
    }
}
