package com.kodegravity.exr.catalog.application;

import com.kodegravity.exr.catalog.domain.ImportAttempt;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Records an import attempt in a transaction of its own.
 *
 * <p>This class exists for exactly one annotation —
 * {@code propagation = REQUIRES_NEW} — and it is a separate bean because that
 * annotation cannot work any other way. The proxy decides whether a new
 * transaction is opened <em>when the call arrives from outside</em>. A
 * {@code this.recordFailure(...)} call from inside {@code EventService} would go
 * straight past the proxy and join the caller's transaction, which is precisely
 * the transaction that is about to be rolled back. Separate bean, real boundary.
 *
 * <p><b>What REQUIRES_NEW buys:</b> the caller's transaction is suspended, a
 * second connection is taken from the pool, and this work is committed on its
 * own. The failed import rolls back; the record of the failure does not.
 *
 * <p><b>What it costs:</b> a second connection held at the same time as the
 * first, and the loss of atomicity — if the caller fails <em>after</em> this
 * commits, there is no way to take the record back. Both are acceptable for a
 * log of failures, and neither is acceptable for money.
 */
@Service
public class ImportAuditService {

    private final EntityManager entityManager;

    public ImportAuditService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Writes one failed attempt and commits it immediately, regardless of what
     * the caller's transaction does next.
     *
     * <p>The {@link Throwable} is taken rather than a formatted string so the
     * message is captured where the exception still has its context — and so the
     * truncation lives in one place, {@link ImportAttempt#fail(String)}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID recordFailure(short sourceId, int eventCount, Throwable failure) {
        ImportAttempt attempt = new ImportAttempt(UUID.randomUUID(), sourceId, eventCount);
        attempt.fail(failure.getMessage());

        entityManager.persist(attempt);
        return attempt.getId();
    }
}
