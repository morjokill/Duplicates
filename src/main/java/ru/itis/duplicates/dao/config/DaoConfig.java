package ru.itis.duplicates.dao.config;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;

//TODO: только 1 раз
public class DaoConfig {
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/duplicates";
    private static final String DATABASE_USER = "duplicates_user";
    private static final String DATABASE_PASSWORD = "duplicates";

    public static DataSource getDataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUsername(DATABASE_USER);
        dataSource.setPassword(DATABASE_PASSWORD);
        dataSource.setUrl(DATABASE_URL);
        return dataSource;
    }
}
