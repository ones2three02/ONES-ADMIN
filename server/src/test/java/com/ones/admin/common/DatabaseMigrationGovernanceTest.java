package com.ones.admin.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseMigrationGovernanceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationHistoryIsCreated() {
        Integer installedRankCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history",
                Integer.class
        );

        assertThat(installedRankCount).isNotNull();
        assertThat(installedRankCount).isGreaterThan(0);
    }
}
