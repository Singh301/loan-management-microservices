package com.loanmanagement.auth.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class AuthSchemaMigrationIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("auth_migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void shouldMigrateRefreshTokensFromPlaintextToSha256Hashes() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .target("1")
                .load();

        flyway.migrate();

        String plaintextToken = "refresh-token-used-only-for-migration-test";
        try (var connection = MYSQL.createConnection("")) {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO users (username, email, password_hash, full_name, role)
                        VALUES ('migration-user', 'migration@example.com', 'test-hash', 'Migration User', 'CUSTOMER')
                        """);

                long userId;
                try (ResultSet rs = statement.executeQuery(
                        "SELECT id FROM users WHERE username = 'migration-user'")) {
                    assertTrue(rs.next());
                    userId = rs.getLong(1);
                }

                try (var insert = connection.prepareStatement(
                        "INSERT INTO refresh_tokens (token, user_id, expiry_date) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 1 DAY))")) {
                    insert.setString(1, plaintextToken);
                    insert.setLong(2, userId);
                    insert.executeUpdate();
                }
            }
        }

        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        try (var connection = MYSQL.createConnection("");
             var statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT token, CHAR_LENGTH(token) FROM refresh_tokens WHERE user_id = " +
                             "(SELECT id FROM users WHERE username = 'migration-user')")) {

            assertTrue(rs.next());
            String storedValue = rs.getString(1);
            int storedLength = rs.getInt(2);

            assertFalse(storedValue.equals(plaintextToken));
            assertEquals(64, storedLength);
            assertEquals(64, storedValue.length());
            assertTrue(storedValue.matches("[0-9a-f]{64}"));
        }
    }
}
