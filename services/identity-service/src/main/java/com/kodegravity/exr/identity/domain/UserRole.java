package com.kodegravity.exr.identity.domain;

/**
 * The role stored in {@code users.role} — one role per user, not a set.
 *
 * <p>The product's ladder (`projectDetails.md`): a guest is not stored at all
 * (no row, no account), then a free user, a paid user, an admin, and a developer
 * with unrestricted access. EXR gives each account exactly ONE rung, so a column
 * holds it; a user-to-role join table would allow combinations the product has
 * no rules for.
 *
 * <p>Two things this enum deliberately does NOT do: it does not model the
 * hierarchy (that is Spring Security's {@code RoleHierarchy}, Day 5.4), and it
 * does not hold the payment tier — a FREE_USER upgrades by changing this value
 * once, but the payment history lives in the payment service.
 */
public enum UserRole {

    /** Registered, unpaid: at most 2 subscriptions. */
    FREE_USER,

    /** Paid: unlimited subscriptions. */
    PAID_USER,

    /** Manages events and users, but never a DEVELOPER account. */
    ADMIN,

    /** Unrestricted: flags, retention tuning, replays. */
    DEVELOPER
}
