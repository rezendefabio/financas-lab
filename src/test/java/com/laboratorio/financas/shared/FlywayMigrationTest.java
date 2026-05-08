package com.laboratorio.financas.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveTerAplicadoMigrationV1ComSucesso() {
        // Given - Flyway aplicou as migrations no startup do contexto Spring

        // When
        Boolean v1Aplicada = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'",
                Boolean.class
        );

        // Then
        assertThat(v1Aplicada).isTrue();
    }

    @Test
    void deveTerCriadoTabelaHealthcheckComLinhaPlaceholder() {
        // When
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM __healthcheck",
                Integer.class
        );

        // Then
        assertThat(total).isGreaterThanOrEqualTo(1);
    }
}
