package com.kodegravity.exr.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * One row of {@code sources} — where catalog data comes from (TMDB, IGDB,
 * the seeded provider, manual admin entry).
 *
 * <p><b>An entity is not a DTO.</b> This object is <em>live</em>: it is attached
 * to a Hibernate session, its fields can change, and changing one schedules an
 * UPDATE. A record used as a DTO is an immutable snapshot. Entities never leave
 * the service — controllers receive records (plan.md rule 6 / section 6).
 *
 * <p><b>Flyway owns this table, Hibernate only describes it.</b> The
 * {@code nullable}/{@code length} values below never create or alter anything;
 * the DDL in {@code V1__catalog_init.sql} is the authority. They are read at
 * startup by {@code ddl-auto: validate}, which fails the boot if this class and
 * the real table disagree.
 */
@Entity
@Table(name = "sources")
public class Source {

    /**
     * {@code smallint} in the DDL, so {@link Short} here — and deliberately an
     * assigned id, not generated: the four source ids are part of the contract
     * and renumbering them would silently re-point existing events.
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Short id;

    /** Short machine name ({@code TMDB}, {@code IGDB}). Immutable once written. */
    @Column(name = "code", nullable = false, length = 20, updatable = false)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * How long we may keep the rich details. A licence rule, which is why it is
     * data an admin can change rather than a constant in {@code application.yml}.
     */
    @Column(name = "details_retention_days", nullable = false)
    private int detailsRetentionDays;

    @Column(name = "cache_ttl_seconds", nullable = false)
    private int cacheTtlSeconds;

    /**
     * {@code EnumType.STRING} writes {@code 'ANONYMISE'}. The default
     * ({@code ORDINAL}) would write {@code 1} — and reordering the enum
     * constants would then silently change the meaning of every stored row.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_policy", nullable = false, length = 20)
    private ExpiryPolicy expiryPolicy = ExpiryPolicy.ANONYMISE;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Stamped by us — see {@link #stampTimestamps()}. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Stamped by us on every update — see {@link #stampTimestamps()}. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Required by JPA. Hibernate builds the object first and fills the fields
     * afterwards, so it needs a no-argument constructor. {@code protected} is
     * enough for that, and stops application code from creating a row with no
     * values.
     */
    protected Source() {
    }

    public Source(short id, String code, String displayName,
                  int detailsRetentionDays, int cacheTtlSeconds, ExpiryPolicy expiryPolicy) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.detailsRetentionDays = detailsRetentionDays;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.expiryPolicy = expiryPolicy;
    }

    /**
     * The application owns the timestamps, not the database.
     *
     * <p>The DDL gives both columns {@code DEFAULT now()}, but V1 creates
     * <b>no trigger</b>, so nothing in Postgres would ever bump
     * {@code updated_at} on an UPDATE. The default only covers rows inserted
     * by hand (the four seed rows). Marking these columns
     * {@code insertable = false, updatable = false} would look tidy and would
     * leave {@code updated_at} permanently stale — the classic "read-only
     * mapping with no trigger" bug.
     */
    @PrePersist
    @PreUpdate
    void stampTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getDetailsRetentionDays() {
        return detailsRetentionDays;
    }

    public void setDetailsRetentionDays(int detailsRetentionDays) {
        this.detailsRetentionDays = detailsRetentionDays;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public ExpiryPolicy getExpiryPolicy() {
        return expiryPolicy;
    }

    public void setExpiryPolicy(ExpiryPolicy expiryPolicy) {
        this.expiryPolicy = expiryPolicy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Identity is the natural key {@code code}, not {@code id}.
     *
     * <p>Comparing on {@code id} looks obvious and is a known trap: a brand-new
     * entity has no id yet, so it equals nothing — put it in a {@link java.util.HashSet}
     * before saving and you will not find it afterwards. {@code code} is unique
     * in the database and never changes, so it is stable in both states.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Source source)) {
            return false;
        }
        return code != null && code.equals(source.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    @Override
    public String toString() {
        return "Source[" + code + "]";
    }
}
