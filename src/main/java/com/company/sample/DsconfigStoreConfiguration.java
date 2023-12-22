package com.company.sample;

import io.jmix.autoconfigure.data.JmixLiquibaseCreator;
import io.jmix.core.JmixModules;
import io.jmix.core.Resources;
import io.jmix.data.impl.JmixEntityManagerFactoryBean;
import io.jmix.data.impl.JmixTransactionManager;
import io.jmix.data.persistence.DbmsSpecifics;
import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@Configuration
public class DsconfigStoreConfiguration {

    @Bean
    @ConfigurationProperties("dsconfig.datasource")
    DataSourceProperties dsconfigDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "dsconfig.datasource.hikari")
    DataSource dsconfigDataSource(@Qualifier("dsconfigDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean dsconfigEntityManagerFactory(
            @Qualifier("dsconfigDataSource") DataSource dataSource,
            JpaVendorAdapter jpaVendorAdapter,
            DbmsSpecifics dbmsSpecifics,
            JmixModules jmixModules,
            Resources resources
    ) {
        return new JmixEntityManagerFactoryBean("dsconfig", dataSource, jpaVendorAdapter, dbmsSpecifics, jmixModules, resources);
    }

    @Bean
    JpaTransactionManager dsconfigTransactionManager(@Qualifier("dsconfigEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JmixTransactionManager("dsconfig", entityManagerFactory);
    }

    @Bean("dsconfigLiquibaseProperties")
    @ConfigurationProperties(prefix = "dsconfig.liquibase")
    public LiquibaseProperties dsconfigLiquibaseProperties() {
        return new LiquibaseProperties();
    }

    @Bean("dsconfigLiquibase")
    public SpringLiquibase dsconfigLiquibase(@Qualifier("dsconfigDataSource") DataSource dataSource,
                                             @Qualifier("dsconfigLiquibaseProperties") LiquibaseProperties liquibaseProperties) {
        return JmixLiquibaseCreator.create(dataSource, liquibaseProperties);
    }
}
