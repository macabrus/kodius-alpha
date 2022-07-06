package com.kodius.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Slf4JSqlLogger;
import org.jdbi.v3.jackson2.Jackson2Config;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.sql.DataSource;

public class DatabaseModule implements Module {

    @Provides
    public DataSource dataSource(HikariConfig config) {
        return new HikariDataSource(config);
    }

    @Provides
    public Jdbi jdbi(DataSource ds, ObjectMapper mapper) {
        /* Configure JDBI interface for simpler DB access */
        var jdbi = Jdbi.create(ds)
            .installPlugin(new SqlObjectPlugin())
            .installPlugin(new Jackson2Plugin());
        jdbi.getConfig(Jackson2Config.class)
            .setMapper(mapper);
        jdbi.setSqlLogger(new Slf4JSqlLogger());
        return jdbi;
    }

    @Provides
    public HikariConfig hikariConfig() {
        /* Configure JDBC driver and connection pooling */
        var config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/kodius");
        config.setUsername("kodius");
        config.setPassword("kodius");
        config.addDataSourceProperty("dataSourceClassName", "org.postgresql.Driver");
        config.addDataSourceProperty("autoCommit", false);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("cachePrepStmts", true);
        return config;
    }

    @Override
    public void configure(Binder binder) {

    }
}
