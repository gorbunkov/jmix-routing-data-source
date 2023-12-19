package com.company.sample.view.customer;

import com.company.sample.entity.Customer;

import com.company.sample.view.main.MainView;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.InstanceNameProvider;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Route(value = "customers", layout = MainView.class)
@ViewController("Customer.list")
@ViewDescriptor("customer-list-view.xml")
@LookupComponent("customersDataGrid")
@DialogMode(width = "64em")
public class CustomerListView extends StandardListView<Customer> {

    @Autowired
    private Dialogs dialogs;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private InstanceNameProvider instanceNameProvider;
    @Autowired
    private Notifications notifications;

    @Subscribe(id = "runBackgroundTaskBtn", subject = "clickListener")
    public void onRunBackgroundTaskBtnClick(final ClickEvent<JmixButton> event) {
        MyBackgroundTask backgroundTask = new MyBackgroundTask();
        dialogs.createBackgroundTaskDialog(backgroundTask)
                .open();
    }

    private class MyBackgroundTask extends BackgroundTask<Integer, String> {

        public MyBackgroundTask() {
            super(1, TimeUnit.MINUTES, CustomerListView.this);
        }

        @Override
        public String run(TaskLifeCycle<Integer> taskLifeCycle) throws Exception {
            List<Customer> customers = dataManager.load(Customer.class).all().list();
            String customerNames = customers.stream()
                    .map(instanceNameProvider::getInstanceName)
                    .collect(Collectors.joining(", "));
            return customerNames;
        }

        @Override
        public void done(String result) {
            notifications.create(result)
                    .show();
        }
    }
}