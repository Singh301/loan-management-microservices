package com.loanmanagement.loan.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class MySqlContainerIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("loan_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void mysqlContainerStartsAndAcceptsConnections() throws Exception {
        try (var connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            assertNotNull(connection);
            assertNotNull(connection.getMetaData());
        }
    }
}
