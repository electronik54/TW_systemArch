package com.kodegravity.exr.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the thing the SQL test could not: that FLYWAY ran, and that it recorded
 * what it did.
 *
 * <p>Starting this test starts a Spring Boot application context. Boot creates the
 * DataSource, then runs Flyway, then builds JPA - so the assertions below run
 * against a database that Flyway has already migrated.
 */
@SpringBootTest
class FlywayMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("every migration is applied once and recorded in flyway_schema_history")
    void migrationIsRecorded() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "select version, description, script, checksum, success "
                             + "from flyway_schema_history order by installed_rank")) {

            List<String> scripts = new ArrayList<>();
            while (rs.next()) {
                scripts.add(rs.getString("script"));

                assertThat(rs.getBoolean("success")).isTrue();
                assertThat(rs.getInt("checksum")).isNotZero();
            }

            // exactly these, in this order, each applied once - not twice, not zero times.
            // V2 is appended, never merged into V1: V1's checksum is already recorded
            // everywhere it has run, so editing it would fail every existing database.
            assertThat(scripts).containsExactly("V1__catalog_init.sql", "V2__event_search.sql", "V3__import_audit.sql");
        }
    }

    @Test
    @DisplayName("the four tables the migration creates are present")
    void tablesExist() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "select count(*) from pg_tables "
                             + "where schemaname = 'public' and tablename in "
                             + "('sources','events','subscriptions','event_reports')")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("V2 adds a generated tsvector column and a GIN index over it")
    void searchVectorIsGeneratedAndIndexed() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // is_generated = ALWAYS means the DATABASE writes this column on every
            // insert and update. If a future migration turned it into a plain
            // column filled by Java, this assertion is what catches it.
            try (ResultSet rs = statement.executeQuery(
                    "select is_generated, generation_expression "
                            + "from information_schema.columns "
                            + "where table_name = 'events' and column_name = 'search_vector'")) {

                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("is_generated")).isEqualTo("ALWAYS");
                assertThat(rs.getString("generation_expression")).contains("to_tsvector");
            }

            // the access method matters as much as the column: a B-tree here would
            // build fine and then never be used by a `@@` filter
            try (ResultSet rs = statement.executeQuery(
                    "select indexdef from pg_indexes "
                            + "where tablename = 'events' and indexname = 'events_search_gin'")) {

                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("indexdef")).contains("USING gin");
            }
        }
    }
}
