package com.parkshare.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * TestContainers configuration that spins up a PostGIS-enabled PostgreSQL container.
 *
 * Any @DataJpaTest or @SpringBootTest that imports this configuration (or extends
 * a base class that does) will automatically get a real PostgreSQL+PostGIS database.
 *
 * Usage:
 *   @Import(TestContainersConfig.class)
 *   @DataJpaTest
 *   class MyRepositoryTest { ... }
 */
@TestConfiguration
@Testcontainers
public class TestContainersConfig {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgis/postgis:16-3.4-alpine")
                    .withDatabaseName("parkshare_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withInitScript("init-postgis.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
