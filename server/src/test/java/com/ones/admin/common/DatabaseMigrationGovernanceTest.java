package com.ones.admin.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseMigrationGovernanceTest {

    private static final Path LEGACY_SCHEMA_FILE = Path.of("src/main/resources/schema.sql");
    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    private static final Pattern VERSIONED_MIGRATION_PATTERN =
            Pattern.compile("^V([1-9][0-9]*)__([a-z0-9]+(?:_[a-z0-9]+)*)\\.sql$");
    private static final Pattern DESTRUCTIVE_SQL_PATTERN = Pattern.compile(
            "\\b(drop\\s+table|truncate\\s+table|delete\\s+from|alter\\s+table\\s+[^;]+\\s+drop\\s+(column\\s+)?)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final String DESTRUCTIVE_SQL_APPROVAL_MARKER = "ONES-MIGRATION-APPROVED-DESTRUCTIVE";

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

    @Test
    void legacySchemaSqlIsNotUsed() {
        assertThat(LEGACY_SCHEMA_FILE).doesNotExist();
    }

    @Test
    void migrationFilesUseStandardNamesAndConsecutiveVersions() throws IOException {
        List<Path> migrations = migrationFiles();

        assertThat(migrations).isNotEmpty();
        List<Integer> versions = migrations.stream()
                .map(this::extractVersion)
                .sorted()
                .toList();
        assertThat(versions).doesNotHaveDuplicates();
        assertThat(versions).containsExactlyElementsOf(
                IntStream.rangeClosed(1, versions.size())
                        .boxed()
                        .toList()
        );
    }

    @Test
    void destructiveMigrationSqlMustBeExplicitlyApproved() throws IOException {
        for (Path migration : migrationFiles()) {
            String sql = Files.readString(migration);
            Matcher matcher = DESTRUCTIVE_SQL_PATTERN.matcher(sql);
            if (matcher.find()) {
                assertThat(sql)
                        .as("%s 包含破坏性 SQL：%s", migration.getFileName(), matcher.group())
                        .contains(DESTRUCTIVE_SQL_APPROVAL_MARKER);
            }
        }
    }

    private List<Path> migrationFiles() throws IOException {
        try (var paths = Files.list(MIGRATION_DIRECTORY)) {
            return paths
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .peek(this::assertStandardName)
                    .toList();
        }
    }

    private void assertStandardName(Path path) {
        String filename = path.getFileName().toString();
        assertThat(filename)
                .as("Flyway 迁移文件必须使用 V版本号__小写下划线说明.sql 命名")
                .matches(VERSIONED_MIGRATION_PATTERN);
    }

    private int extractVersion(Path path) {
        Matcher matcher = VERSIONED_MIGRATION_PATTERN.matcher(path.getFileName().toString());
        assertThat(matcher.matches()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }
}
