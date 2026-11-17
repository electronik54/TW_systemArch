package com.kodegravity.exr.identity.domain;

/**
 * The account states stored in {@code users.status}.
 *
 * <p>Why an enum and not a boolean: "cannot log in" has three different causes,
 * and an admin needs to tell them apart. Locked is temporary and usually
 * automatic (too many failed attempts, Day 4.3). Suspended is a decision a human
 * made. Enabled is the normal state. A single {@code locked = true} column
 * collapses those two meanings into one and the audit trail loses the difference.
 *
 * <p>Stored as the constant NAME, never its position — the same rule as the
 * catalogue's {@code sources.expiry_policy}: reordering the constants would
 * otherwise silently re-point every stored row.
 */
public enum UserStatus {

    /** Normal. May authenticate and use the product. */
    ENABLED,

    /** Temporarily barred, typically after repeated failed logins. Reversible. */
    LOCKED,

    /** Barred by a human, usually after an abuse report. Reinstatement is deliberate. */
    SUSPENDED
}
