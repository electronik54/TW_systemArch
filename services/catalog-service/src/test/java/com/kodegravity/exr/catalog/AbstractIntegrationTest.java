package com.kodegravity.exr.catalog;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Every integration test in this service starts here.
 *
 * <p>What it buys: the test suite no longer needs a database that somebody
 * prepared by hand. It starts its own Postgres — same image as the dev
 * container, so the same engine, the same <code>text[]</code>, the same
 * <code>char(2)</code>, the same GIN indexes — and then throws it away.
 *
 * <p>Four moving parts, in the order they happen:
 * <ol>
 *   <li>the container is declared as a <b>static</b> field, so it is created
 *       once per JVM rather than once per test class;</li>
 *   <li>the static block starts it immediately — it must already be running
 *       before Spring resolves a datasource URL;</li>
 *   <li>{@code @DynamicPropertySource} hands Spring the random port, the user
 *       and the password, overriding the {@code localhost:55432} values in
 *       {@code application.yml};</li>
 *   <li>Flyway then runs {@code V1__catalog_init.sql} against the empty
 *       container, so the schema is built by the same migration as production.</li>
 * </ol>
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    /** Pinned, and identical to the dev container: a moving tag would hide upgrades. */
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("catalog")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        POSTGRES.start();
        System.out.println("=== TEST POSTGRES ==" + POSTGRES.getJdbcUrl()
                + "  (not 55432 - that is your exr-pg container, and it is untouched)");
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
