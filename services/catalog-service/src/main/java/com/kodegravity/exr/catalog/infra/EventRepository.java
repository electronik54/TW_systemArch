package com.kodegravity.exr.catalog.infra;

import com.kodegravity.exr.catalog.domain.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * A Spring Data repository: we declare the method, Spring writes the class.
 *
 * <p>The two finders below return exactly the same rows. The difference is how
 * many SQL statements each one costs, which is the whole point of this file.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Loads the events, and nothing else. Each event's {@code source} stays a
     * lazy placeholder, so reading {@code event.getSource().getCode()} fires one
     * extra SELECT <em>per event</em>. 1 statement becomes 1 + N.
     *
     * <p>This is not a bug on its own — it is fine when you only need the rows.
     * It becomes the N+1 problem the moment a loop touches the association.
     */
    List<Event> findByCategory(String category);

    /**
     * Same rows, one statement. {@code attributePaths = "source"} tells Spring
     * Data to build a JOIN FETCH, so the supplier rows come back in the same
     * result set and the lazy placeholder is never created.
     *
     * <p>Read it as "fetch the graph of objects I am about to use". The cost is
     * honest: more columns over the wire and duplicate event columns for events
     * that share a supplier. For a many-to-one that is a cheap trade.
     */
    @EntityGraph(attributePaths = "source")
    List<Event> findWithSourceByCategory(String category);
}
