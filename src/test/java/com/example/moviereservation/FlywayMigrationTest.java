package com.example.moviereservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs db/migration on an empty DB, then Hibernate validates every entity against it
 * (what prod does on boot). Fails if an entity changes without a new V*__ migration.
 * shortcut: H2 in PostgreSQL mode, not real Postgres; switch to Testcontainers if the SQL gets Postgres-specific.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flywaycheck;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Test
    void migrationsMatchEntities() {
        // context startup is the assertion: Flyway migrate + Hibernate validate
    }
}
