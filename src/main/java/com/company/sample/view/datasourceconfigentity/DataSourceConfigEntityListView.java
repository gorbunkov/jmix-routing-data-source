package com.company.sample.view.datasourceconfigentity;

import com.company.sample.datasource.DataSourceRepository;
import com.company.sample.entity.DataSourceConfigEntity;
import com.company.sample.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntitySet;
import io.jmix.core.EntityStates;
import io.jmix.core.SaveContext;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.*;
import io.jmix.flowui.util.RemoveOperation;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Set;

@Route(value = "dataSourceConfigEntities", layout = MainView.class)
@ViewController("DataSourceConfigEntity.list")
@ViewDescriptor("data-source-config-entity-list-view.xml")
@LookupComponent("dataSourceConfigEntitiesDataGrid")
@DialogMode(width = "64em")
public class DataSourceConfigEntityListView extends StandardListView<DataSourceConfigEntity> {

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<DataSourceConfigEntity> dataSourceConfigEntitiesDc;

    @ViewComponent
    private InstanceContainer<DataSourceConfigEntity> dataSourceConfigEntityDc;

    @ViewComponent
    private InstanceLoader<DataSourceConfigEntity> dataSourceConfigEntityDl;

    @ViewComponent
    private VerticalLayout listLayout;

    @ViewComponent
    private FormLayout form;

    @ViewComponent
    private HorizontalLayout detailActions;
    @Autowired
    private DataSourceRepository dataSourceRepository;
    @Autowired
    private EntityStates entityStates;

    @Subscribe
    public void onInit(final InitEvent event) {
        updateControls(false);
    }

    @Subscribe("dataSourceConfigEntitiesDataGrid.create")
    public void onDataSourceConfigEntitiesDataGridCreate(final ActionPerformedEvent event) {
        dataContext.clear();
        DataSourceConfigEntity entity = dataContext.create(DataSourceConfigEntity.class);
        dataSourceConfigEntityDc.setItem(entity);
        updateControls(true);
    }

    @Subscribe("dataSourceConfigEntitiesDataGrid.edit")
    public void onDataSourceConfigEntitiesDataGridEdit(final ActionPerformedEvent event) {
        updateControls(true);
    }

    @Subscribe("saveBtn")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        dataContext.save();
        dataSourceConfigEntitiesDc.replaceItem(dataSourceConfigEntityDc.getItem());
        updateControls(false);
    }

    @Subscribe("cancelBtn")
    public void onCancelButtonClick(final ClickEvent<JmixButton> event) {
        dataContext.clear();
        dataSourceConfigEntityDl.load();
        updateControls(false);
    }

    @Subscribe(id = "dataSourceConfigEntitiesDc", target = Target.DATA_CONTAINER)
    public void onDataSourceConfigEntitiesDcItemChange(final InstanceContainer.ItemChangeEvent<DataSourceConfigEntity> event) {
        DataSourceConfigEntity entity = event.getItem();
        dataContext.clear();
        if (entity != null) {
            dataSourceConfigEntityDl.setEntityId(entity.getId());
            dataSourceConfigEntityDl.load();
        } else {
            dataSourceConfigEntityDl.setEntityId(null);
            dataSourceConfigEntityDc.setItem(null);
        }
    }

    private void updateControls(boolean editing) {
        form.getChildren().forEach(component -> {
            if (component instanceof HasValueAndElement<?, ?> field) {
                field.setReadOnly(!editing);
            }
        });

        detailActions.setVisible(editing);
        listLayout.setEnabled(!editing);
    }

    @Install(to = "dataSourceConfigEntitiesDataGrid.remove", subject = "afterActionPerformedHandler")
    private void dataSourceConfigEntitiesDataGridRemoveAfterActionPerformedHandler(final RemoveOperation.AfterActionPerformedEvent<DataSourceConfigEntity> afterActionPerformedEvent) {
        for (DataSourceConfigEntity dataSourceConfigEntity : afterActionPerformedEvent.getItems()) {
            dataSourceRepository.removeDatasource(dataSourceConfigEntity.getName());
        }
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onPostSave(final DataContext.PostSaveEvent event) {
        EntitySet savedInstances = event.getSavedInstances();
        Collection<DataSourceConfigEntity> sourceConfigEntityCollection = savedInstances.getAll(DataSourceConfigEntity.class);
        for (DataSourceConfigEntity dataSourceConfigEntity : sourceConfigEntityCollection) {
            dataSourceRepository.updateDatasource(dataSourceConfigEntity);
        }
    }
}