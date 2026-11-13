package com.kodegravity.exr.catalog;

import com.kodegravity.exr.catalog.domain.Event;
import com.kodegravity.exr.catalog.domain.Source;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shows what {@code events.source_id} actually is from Java's point of view:
 * not a number you look up, but the supplier row itself.
 */
@SpringBootTest(properties = "spring.jpa.show-sql=true")
@Transactional
class EventMappingTest extends AbstractIntegrationTest {

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

    /**
     * The rest of the row in one pass: a Postgres array, the version counter, and
     * the timestamps the application stamps for itself.
     */
    @Test
    void arrays_version_and_timestamps_round_trip() {
        Source igdb = entityManager.find(Source.class, (short) 2);

        Event event = new Event(UUID.randomUUID(), igdb, "ext-" + UUID.randomUUID(), "GAME",
                "Hollow Knight: Silksong", LocalDate.of(2026, 6, 12), "DAY");
        event.setSynopsis("A bug with a needle.");
        event.setGenres("Metroidvania", "Action-Adventure");
        event.setPlatforms("PC", "Nintendo Switch");
        event.setCountry("US");

        entityManager.persist(event);
        entityManager.flush();   // a real INSERT: version 0, both timestamps filled
        entityManager.clear();

        Event reloaded = entityManager.find(Event.class, event.getId());
        Instant createdAt = reloaded.getCreatedAt();

        System.out.println("=== GENRES ========== " + Arrays.toString(reloaded.getGenres()));
        System.out.println("=== PLATFORMS ======= " + Arrays.toString(reloaded.getPlatforms()));
        System.out.println("=== STATUS ========== " + reloaded.getStatus());
        System.out.println("=== AVAILABILITY ==== " + reloaded.getAvailability());
        System.out.println("=== COUNTRY ========= " + reloaded.getCountry());
        System.out.println("=== VERSION ========= " + reloaded.getVersion());
        System.out.println("=== CREATED AT ====== " + createdAt);

        // One text[] column, read back as a Java array.
        assertThat(reloaded.getGenres()).containsExactly("Metroidvania", "Action-Adventure");
        assertThat(reloaded.getPlatforms()).containsExactly("PC", "Nintendo Switch");
        // Nobody set these two: the field initialisers did, because the DDL's
        // DEFAULT never fires for a column Hibernate includes in the INSERT.
        assertThat(reloaded.getStatus()).isEqualTo("ANNOUNCED");
        assertThat(reloaded.getAvailability()).isEqualTo("FRESH");
        assertThat(reloaded.getCountry()).isEqualTo("US");
        // A new row starts at version 0, not 1.
        assertThat(reloaded.getVersion()).isZero();
        assertThat(createdAt).isNotNull();

        reloaded.setTitle("Hollow Knight: Silksong (delayed)");
        entityManager.flush();   // look for "and version=?" in the UPDATE below

        System.out.println("=== VERSION AFTER UPDATE = " + reloaded.getVersion());
        System.out.println("=== TITLE NOW =========== " + reloaded.getTitle());

        assertThat(reloaded.getVersion()).isEqualTo(1);
        // Mapped updatable = false, so the UPDATE cannot rewrite it.
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }
}
