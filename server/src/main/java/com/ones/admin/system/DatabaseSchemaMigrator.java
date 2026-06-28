package com.ones.admin.system;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@Order(0)
public class DatabaseSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaMigrator(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        addColumnIfMissing("sys_user", "dept_id", "alter table sys_user add column dept_id bigint");
        addColumnIfMissing("sys_user", "remark", "alter table sys_user add column remark varchar(255)");
        addColumnIfMissing("sys_user", "failed_login_count", "alter table sys_user add column failed_login_count int not null default 0");
        addColumnIfMissing("sys_user", "locked_until", "alter table sys_user add column locked_until timestamp null");
        addColumnIfMissing("sys_user", "last_login_at", "alter table sys_user add column last_login_at timestamp null");
        addColumnIfMissing("sys_role", "remark", "alter table sys_role add column remark varchar(255)");
    }

    private void addColumnIfMissing(String tableName, String columnName, String sql) throws SQLException {
        if (hasColumn(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute(sql);
    }

    private boolean hasColumn(String tableName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return hasColumn(metaData, tableName, columnName)
                    || hasColumn(metaData, tableName.toUpperCase(), columnName.toUpperCase())
                    || hasColumn(metaData, tableName.toLowerCase(), columnName.toLowerCase());
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
