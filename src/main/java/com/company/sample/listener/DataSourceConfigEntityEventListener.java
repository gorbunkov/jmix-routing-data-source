package com.company.sample.listener;

import com.company.sample.datasource.DataSourceRepository;
import com.company.sample.entity.DataSourceConfigEntity;
import io.jmix.core.DataManager;
import io.jmix.core.event.EntityChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener keeps the {@link DataSourceRepository} up to date.
 */
@Component
public class DataSourceConfigEntityEventListener {

    private final DataManager dataManager;
    private final DataSourceRepository dataSourceRepository;

    public DataSourceConfigEntityEventListener(DataSourceRepository dataSourceRepository, DataManager dataManager) {
        this.dataSourceRepository = dataSourceRepository;
        this.dataManager = dataManager;
    }

    @TransactionalEventListener
    public void onDataSourceConfigEntityChangedAfterCommit(final EntityChangedEvent<DataSourceConfigEntity> event) {
        DataSourceConfigEntity datasourceConfigEntity = dataManager.load(event.getEntityId())
                .joinTransaction(false)
                .one();
        switch (event.getType()) {
            case CREATED -> dataSourceRepository.addDatasource(datasourceConfigEntity);
            case UPDATED -> dataSourceRepository.updateDatasource(datasourceConfigEntity);
            case DELETED -> dataSourceRepository.removeDatasource(datasourceConfigEntity.getName());
        }
    }
}