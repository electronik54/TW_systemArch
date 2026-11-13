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
    @DisplayName("V1 is applied once and recorded in flyway_schema_history")
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

            // exactly one migration, applied once - not twice, not zero times
            assertThat(scripts).containsExactly("V1__catalog_init.sql");
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
}
