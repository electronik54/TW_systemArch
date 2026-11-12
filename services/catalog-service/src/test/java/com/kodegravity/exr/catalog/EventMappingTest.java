package com.kodegravity.exr.catalog;

import com.kodegravity.exr.catalog.domain.Event;
import com.kodegravity.exr.catalog.domain.Source;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shows what {@code events.source_id} actually is from Java's point of view:
 * not a number you look up, but the supplier row itself.
 */
@SpringBootTest(properties = "spring.jpa.show-sql=true")
@Transactional
class EventMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void a_foreign_key_is_the_other_row_not_a_number() {
        Source igdb = entityManager.find(Source.class, (short) 2);

        Event event = new Event(UUID.randomUUID(), igdb, "ext-123", "GAME",
                "Hollow Knight: Silksong", LocalDate.of(2026, 6, 12), "DAY");

        entityManager.persist(event);   // write it to Postgres now
        entityManager.flush();          // ...as an actual INSERT
        entityManager.clear();          // make Hibernate forget everything it holds

        // Everything below is read back from the database, into fresh objects.
        Event reloaded = entityManager.find(Event.class, event.getId());

        System.out.println("=== TITLE ============ " + reloaded.getTitle());
        System.out.println("=== RELEASE DATE ===== " + reloaded.getReleaseDate());
        System.out.println("=== WHO SUPPLIED IT == " + reloaded.getSource().getDisplayName());
        System.out.println("=== ...AND ITS CODE == " + reloaded.getSource().getCode());

        assertThat(reloaded.getSource().getCode()).isEqualTo("IGDB");
    }
}
