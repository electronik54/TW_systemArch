package com.kodegravity.exr.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One row of {@code import_attempts} — the record that must outlive the import
 * it describes (V3).
 *
 * <p><b>Why this entity holds a bare {@code short sourceId} while
 * {@link Event} holds a {@link Source} object.</b> Two different jobs. An event
 * navigates: the screen needs the supplier's display name, so a
 * {@code @ManyToOne} saves a lookup. An attempt is written at the moment
 * something has just gone wrong — the fewer moving parts involved, the fewer
 * ways for the audit write to fail for its own reasons. There is no navigation
 * to do with it, so the column is read as what it is: a number, checked by a
 * foreign key that Postgres enforces.
 *
 * <p><b>Timestamps are ours again</b>, for the same reason as everywhere else in
 * this schema: V3 declares {@code DEFAULT now()} but creates no trigger, so the
 * default only covers rows written by hand.
 */
@Entity
@Table(name = "import_attempts")
public class ImportAttempt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "source_id", nullable = false, updatable = false)
    private short sourceId;

    /** How many items the batch carried — the difference between a data bug and an outage. */
    @Column(name = "event_count", nullable = false, updatable = false)
    private int eventCount;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    /** {@code null} while the attempt is still running. */
    @Column(name = "failed_at")
    private Instant failedAt;

    /** Already truncated by {@link #fail(String)} — see there for why. */
    @Column(name = "error", columnDefinition = "text")
    private String error;

    protected ImportAttempt() {
    }

    public ImportAttempt(UUID id, short sourceId, int eventCount) {
        this.id = id;
        this.sourceId = sourceId;
        this.eventCount = eventCount;
    }

    /**
     * Marks the attempt as failed.
     *
     * <p>The message is cut to 1000 characters on purpose. A Postgres error for a
     * constraint violation carries the offending row's values, and this table is
     * meant to record <em>what went wrong</em>, not to become a second, unmanaged
     * copy of the data — including synopsis text that the retention policy may
     * have required us to purge.
     */
    public void fail(String message) {
        this.failedAt = Instant.now();
        this.error = message == null || message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    @PrePersist
    void stampStartedAt() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public short getSourceId() {
        return sourceId;
    }

    public int getEventCount() {
        return eventCount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getError() {
        return error;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImportAttempt attempt)) {
            return false;
        }
        return id != null && id.equals(attempt.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ImportAttempt[" + sourceId + " " + eventCount + " items, failed=" + failedAt + "]";
    }
}
