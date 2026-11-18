package com.kodegravity.exr.catalog.application;

import com.kodegravity.exr.catalog.domain.Event;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The catalogue's first service, and the first place in this project where a
 * transaction boundary exists.
 *
 * <p><b>Why this package is called {@code application}.</b> The project is laid
 * out in four layers: {@code api} (HTTP and DTO records) → {@code application}
 * (use cases and transaction boundaries) → {@code domain} (entities and rules,
 * with no Spring imports) → {@code infra} (repositories and outbound clients).
 * A transaction is a use-case concern, not a business rule, so it belongs here
 * and not in {@code domain}, which must stay readable without Spring in mind.
 *
 * <p><b>Why a service and not a controller or a repository.</b> A transaction
 * should wrap one business action — "import these events" — because that is the
 * only unit where "all of it or none of it" means something. A controller would
 * make the unit an HTTP request, so a retry, a redirect or a call from another
 * service would each start a different transaction. A repository would make the
 * unit a single statement, which is too small: two writes that belong together
 * would commit separately, and a failure between them would leave half the work
 * in the database for ever.
 *
 * <p>This is why {@code @Transactional} is <b>not</b> on
 * {@code CatalogServiceApplication} or on the controllers: the annotation marks
 * the seam where a business promise is kept, and only the service layer knows
 * where that seam is.
 *
 * <p><b>What the annotation does at run time.</b> Spring wraps this bean in a
 * stand-in object (a <em>proxy</em>) that the caller really talks to. The proxy
 * opens one connection, turns off autocommit, calls the method, and then commits
 * or rolls back according to how the method ended.
 *
 * <p><b>Two consequences worth remembering before the next step.</b> The
 * annotation only takes effect when the call arrives from <em>outside</em> the
 * bean — a call from one method to another inside this same class goes straight
 * past the proxy and gets no transaction. And the connection is held for the
 * whole method, so a slow call inside a transaction (an HTTP request to TMDB,
 * for example) holds a database connection that everybody else is waiting for.
 */
@Service
public class EventService {

    private final EntityManager entityManager;
    private final ImportAuditService auditService;

    public EventService(EntityManager entityManager, ImportAuditService auditService) {
        this.entityManager = entityManager;
        this.auditService = auditService;
    }

    /**
     * Imports a batch of events as one unit of work, and records the attempt when
     * it fails.
     *
     * <p>Two events that clash on {@code (source_id, external_id)} must leave
     * <b>nothing</b> behind — not the first event that was already accepted. That
     * promise is the whole reason this method is transactional, and it is what
     * {@code EventServiceTransactionTest} proves by watching the table afterwards.
     *
     * <p><b>Why the {@code flush()} is not decoration.</b> Without it, nothing
     * reaches Postgres until the proxy commits, which happens <em>after</em> this
     * method has returned — so the clash is thrown outside the method and the
     * {@code catch} below would never run. The flush moves the INSERTs forward on
     * purpose: the failure surfaces here, where it can still be recorded. The
     * cost is one round trip, and the benefit is that the record of the failure
     * exists at all.
     *
     * <p><b>Why {@code recordFailure} is a separate bean.</b> It is annotated
     * {@code REQUIRES_NEW}, so it must be called through a proxy. Called from
     * inside this class it would join <em>this</em> transaction — the one about to
     * be rolled back — and the audit row would vanish with the events.
     *
     * <p>The exception is rethrown unchanged: recording the failure must not turn
     * a broken import into a successful one.
     *
     * <p>The method takes entities rather than a DTO on purpose: converting an
     * external payload into an {@code Event} is the caller's job (the ingestion
     * layer), and the service's job is only the unit of work. Entities never leave
     * this layer — the API layer maps them to records.
     */
    @Transactional
    public List<UUID> importAll(short sourceId, List<Event> events) {
        List<UUID> imported = new ArrayList<>(events.size());

        try {
            for (Event event : events) {
                entityManager.persist(event);
                imported.add(event.getId());
            }
            entityManager.flush();
        } catch (RuntimeException failure) {
            auditService.recordFailure(sourceId, events.size(), failure);
            throw failure;
        }

        return imported;
    }
}
