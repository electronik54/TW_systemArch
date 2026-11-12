package com.kodegravity.exr.catalog.domain;

/**
 * What happens to an event's rich details once its source's retention clock
 * runs out (catalog/schema.md §8).
 *
 * <p>Stored as the constant NAME, never as its position — see the
 * {@code @Enumerated(EnumType.STRING)} on {@link Source#getExpiryPolicy()}.
 * The allowed set is also enforced in the database by the
 * {@code sources_policy_chk} CHECK constraint, so a bug here cannot store a
 * value that the application is then unable to read back.
 */
public enum ExpiryPolicy {

    /** Purge nothing; simply stop refreshing from the source. */
    KEEP_TITLE,

    /** Null the details and replace the title with a placeholder. */
    ANONYMISE,

    /** Remove the row entirely (its subscriptions cascade away). */
    DELETE_ALL
}
