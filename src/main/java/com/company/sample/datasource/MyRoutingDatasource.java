package com.company.sample.datasource;

import io.jmix.core.session.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.web.context.request.RequestContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Routing datasource that returns a connection to a datasource based on the value of the session parameter
 * {@link #DATA_SOURCE_NAME_PARAMETER}. Datasource with the given name is taken from the {@link DataSourceRepository}.
 * Is no session attribute is defined then the default datasource is used.
 */
public class MyRoutingDatasource extends AbstractDataSource {

    private static final Logger log = LoggerFactory.getLogger(MyRoutingDatasource.class);

    public static final String DATA_SOURCE_NAME_PARAMETER = "dataSourceName";

    private final DataSourceRepository datasourceRepository;

    private final ObjectProvider<SessionData> sessionDataProvider;

    public MyRoutingDatasource(DataSourceRepository datasourceRepository, ObjectProvider<SessionData> sessionDataProvider) {
        this.datasourceRepository = datasourceRepository;
        this.sessionDataProvider = sessionDataProvider;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource dataSource = null;
        boolean isInSessionScope = RequestContextHolder.getRequestAttributes() != null;
        String dataSourceName = isInSessionScope ?
                (String) sessionDataProvider.getObject().getAttribute(DATA_SOURCE_NAME_PARAMETER) :
                null;
        if (dataSourceName != null) {
            dataSource = datasourceRepository.getDatasource(dataSourceName);
            if (dataSource == null) {
                log.error("Datasource {} not found. A default datasource will be used", dataSourceName);
            }
        } else {
            log.debug("Session parameter {} not found. A default datasource will be used", DATA_SOURCE_NAME_PARAMETER);
        }

        if (dataSource == null) {
            dataSource = datasourceRepository.getDefaultDataSource();
        }
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }
}
