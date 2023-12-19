package com.company.sample;

import com.company.sample.datasource.DataSourceRepository;
import com.company.sample.datasource.MyRoutingDatasource;
import com.google.common.base.Strings;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.core.session.SessionData;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Push
@Theme(value = "routing-data-source")
@PWA(name = "Routing Data Source", shortName = "Routing Data Source")
@SpringBootApplication
public class RoutingDataSourceApplication implements AppShellConfigurator {

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(RoutingDataSourceApplication.class, args);
    }

    @Bean
    @Primary
    @ConfigurationProperties("main.datasource")
    DataSourceProperties defaultDataSourceProperties() {
        return new DataSourceProperties();
    }
//
//    @Bean
//    @Primary
//    @ConfigurationProperties("main.datasource.hikari")
//    DataSource dataSource(final DataSourceProperties dataSourceProperties) {
//        return dataSourceProperties.initializeDataSourceBuilder().build();
//    }

    @Bean
    DataSourceRepository datasourceRepository(DataSourceProperties defaultDatasourceProperties) {
        DataSource defaultDatasource = defaultDatasourceProperties.initializeDataSourceBuilder().build();
        return new DataSourceRepository(defaultDatasource);
//        return new DataSourceRepository(dataManager, systemAuthenticator, defaultDatasource);
    }

    @Bean
    @Primary
    DataSource dataSource(DataSourceRepository datasourceRepository,
                          ObjectProvider<SessionData> sessionDataProvider) {
        return new MyRoutingDatasource(datasourceRepository, sessionDataProvider);
    }

    @EventListener
    public void printApplicationUrl(final ApplicationStartedEvent event) {
        LoggerFactory.getLogger(RoutingDataSourceApplication.class).info("Application started at "
                + "http://localhost:"
                + environment.getProperty("local.server.port")
                + Strings.nullToEmpty(environment.getProperty("server.servlet.context-path")));
    }
}
