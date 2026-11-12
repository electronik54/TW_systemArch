package com.kodegravity.exr.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The catalog service entry point.
 *
 * <p>Nothing interesting happens here on purpose. What matters is what Spring Boot
 * does <em>around</em> this class on startup, in this order:
 *
 * <pre>
 *   1. create the DataSource            (connection pool to PostgreSQL)
 *   2. run Flyway                       (the schema is now correct)
 *   3. create the JPA EntityManagerFactory (Hibernate maps entities onto that schema)
 *   4. start the web layer
 * </pre>
 *
 * Flyway running BEFORE Hibernate is why {@code ddl-auto: validate} is safe: by the
 * time Hibernate looks, the tables already exist and are exactly right.
 */
@SpringBootApplication
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
