package com.kodegravity.exr.catalog;

import com.kodegravity.exr.catalog.domain.ExpiryPolicy;
import com.kodegravity.exr.catalog.domain.Source;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole point of this test is what it PRINTS, not what it asserts:
 * a row that lives in Postgres, seen from Java as a normal object.
 */
@SpringBootTest(properties = {
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true"
})
@Transactional
class SourceMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void a_database_row_becomes_a_java_object() {
        Source result = entityManager.find(Source.class, (short) 3);

        System.out.println("=== ROW BECAME OBJECT === " + result);
        System.out.println("=== ITS FIELDS ========= "
                + result.getDisplayName() + " | " + result.getExpiryPolicy()
                + " | " + result.getDetailsRetentionDays() + " days | enabled=" + result.isEnabled());

        assertThat(result.getCode()).isEqualTo("SEED");
        assertThat(result.getExpiryPolicy()).isEqualTo(ExpiryPolicy.KEEP_TITLE);
    }
}
