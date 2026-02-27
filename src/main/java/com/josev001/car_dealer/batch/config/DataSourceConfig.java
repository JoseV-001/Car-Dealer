package com.josev001.car_dealer.batch.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class DataSourceConfig {

    // Cria as propriedades do datasource usado pelo Spring Batch.
    @Bean(defaultCandidate = false)
    @BatchDataSource
    @ConfigurationProperties("app.batch.datasource")
    public DataSourceProperties batchDataSourceProperties() {
        return new DataSourceProperties();
    }

    // Inicializa o datasource do batch a partir das propriedades configuradas.
    @Bean(defaultCandidate = false)
    @BatchDataSource
    public DataSource batchDataSource(
            @Qualifier("batchDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

}
