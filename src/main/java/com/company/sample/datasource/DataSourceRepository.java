package com.company.sample.datasource;

import com.company.sample.entity.DataSourceConfigEntity;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class initializes and stores a list of {@link DataSource}s built from the list of {@link DataSourceConfigEntity}s.
 */
public class DataSourceRepository implements ApplicationContextAware {

    private final static Logger log = LoggerFactory.getLogger(DataSourceRepository.class);

    private final Map<String, DataSource> nameToDatasourceMap = new ConcurrentHashMap<>();

    private final DataSource defaultDataSource;

    private ApplicationContext applicationContext;

    private boolean initialized = false;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public DataSourceRepository(DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
    }

    private void init() {
        SystemAuthenticator systemAuthenticator = applicationContext.getBean(SystemAuthenticator.class);
        systemAuthenticator.runWithSystem(() -> {
            DataManager dataManager = applicationContext.getBean(DataManager.class);
            List<DataSourceConfigEntity> dataSourceConfigEntities = dataManager.load(DataSourceConfigEntity.class).all().list();
            for (DataSourceConfigEntity datasourceConfigEntity : dataSourceConfigEntities) {
                try {
                    nameToDatasourceMap.put(datasourceConfigEntity.getName(), createDataSource(datasourceConfigEntity));
                } catch (Exception e) {
                    log.error("Error creating datasource for connection {}", datasourceConfigEntity.getName(), e);
                }
            }
        });
        log.info("DataSourceRepository successfully initialized");
    }

    private void checkInitialized() {
        if (!initialized) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (!initialized) {
                    init();
                    initialized = true;
                }
            } finally {
                lock.readLock().lock();
                lock.writeLock().unlock();
            }
        }
    }

    @Nullable
    public DataSource getDatasource(String name) {
        lock.readLock().lock();
        try {
            checkInitialized();
            return nameToDatasourceMap.get(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    public DataSource getDefaultDataSource() {
        return defaultDataSource;
    }

    public List<String> getDatasourceNames() {
        lock.readLock().lock();
        try {
            checkInitialized();
            return nameToDatasourceMap.keySet().stream().sorted().toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    private DataSource createDataSource(DataSourceConfigEntity datasourceConfigEntity) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(datasourceConfigEntity.getUrl());
        hikariConfig.setUsername(datasourceConfigEntity.getUsername());
        hikariConfig.setPassword(datasourceConfigEntity.getPassword());

        return new HikariDataSource(hikariConfig);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void addDatasource(DataSourceConfigEntity datasourceConfigEntity) {
        lock.writeLock().lock();
        try {
            nameToDatasourceMap.put(datasourceConfigEntity.getName(), createDataSource(datasourceConfigEntity));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeDatasource(String name) {
        lock.writeLock().lock();
        try {
            DataSource dataSource = nameToDatasourceMap.get(name);
            if (dataSource != null) {
                if (dataSource instanceof HikariDataSource hikariDs) {
                    //close all active connections in the pool
                    hikariDs.close();
                }
            }
            nameToDatasourceMap.remove(name);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateDatasource(DataSourceConfigEntity datasourceConfigEntity) {
        removeDatasource(datasourceConfigEntity.getName());
        addDatasource(datasourceConfigEntity);
    }
}
