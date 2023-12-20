package com.company.sample.view.login;

import com.company.sample.datasource.DataSourceRepository;
import com.company.sample.datasource.MyRoutingDatasource;
import com.company.sample.entity.DataSourceConfigEntity;
import com.google.common.base.Strings;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.CoreProperties;
import io.jmix.core.MessageTools;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.session.SessionData;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.component.textfield.JmixPasswordField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.sys.AppCookies;
import io.jmix.flowui.view.*;
import io.jmix.securityflowui.authentication.AuthDetails;
import io.jmix.securityflowui.authentication.LoginViewSupport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Route(value = "login")
@ViewController("LoginView")
@ViewDescriptor("login-view.xml")
public class LoginView extends StandardView implements LocaleChangeObserver {

    private static final Logger log = LoggerFactory.getLogger(LoginView.class);

    /**
     * Name of the cookie that stores the last used data source configuration name.
     */
    private static final String LAST_DATA_SOURCE_CONFIG_NAME_COOKIE = "jmixLastDataSourceConfigName";

    @ViewComponent
    private H2 loginFormTitle;
    @ViewComponent
    private TypedTextField<String> usernameField;
    @ViewComponent
    private JmixPasswordField passwordField;
    @ViewComponent
    private JmixCheckbox rememberMe;
    @ViewComponent
    private JmixSelect<Locale> localeSelect;
    @ViewComponent
    private JmixButton submitBtn;
    @ViewComponent
    private Div errorMessage;
    @ViewComponent
    private H5 errorMessageTitle;
    @ViewComponent
    private Paragraph errorMessageDescription;

    @Autowired
    private CoreProperties coreProperties;
    @Autowired
    private LoginViewSupport loginViewSupport;
    @Autowired
    private MessageBundle messageBundle;
    @Autowired
    private MessageTools messageTools;

    @Value("${ui.login.defaultUsername:}")
    private String defaultUsername;

    @Value("${ui.login.defaultPassword:}")
    private String defaultPassword;

    @ViewComponent
    private JmixComboBox<String> databaseConnectionField;
    @Autowired
    private SessionData sessionData;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private DataSourceRepository dataSourceRepository;

    protected AppCookies cookies;


    @Subscribe
    public void onInit(final InitEvent event) {
        initLocales();
        initDefaultCredentials();
        initDatabaseConnectionField();
    }

    protected void initLocales() {
        LinkedHashMap<Locale, String> locales = coreProperties.getAvailableLocales().stream()
                .collect(Collectors.toMap(Function.identity(), messageTools::getLocaleDisplayName, (s1, s2) -> s1,
                        LinkedHashMap::new));

        ComponentUtils.setItemsMap(localeSelect, locales);

        localeSelect.setValue(VaadinSession.getCurrent().getLocale());
    }

    protected void initDefaultCredentials() {
        if (StringUtils.isNotBlank(defaultUsername)) {
            usernameField.setTypedValue(defaultUsername);
        }

        if (StringUtils.isNotBlank(defaultPassword)) {
            passwordField.setValue(defaultPassword);
        }
    }

    @Subscribe("submitBtn")
    public void onSubmitBtnClick(final ClickEvent<JmixButton> event) {
        errorMessage.setVisible(false);

        String username = usernameField.getValue();
        String password = passwordField.getValue();

        if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password)) {
            return;
        }

        errorMessage.setVisible(false);

        try {
            loginViewSupport.authenticate(
                    AuthDetails.of(username, password)
                            .withLocale(localeSelect.getValue())
                            .withRememberMe(rememberMe.getValue())
            );

            String dataSourceConfigName = databaseConnectionField.getValue();
            sessionData.setAttribute(MyRoutingDatasource.DATA_SOURCE_NAME_PARAMETER, dataSourceConfigName);
            getCookies().addCookie(LAST_DATA_SOURCE_CONFIG_NAME_COOKIE, Base64.getEncoder().encodeToString(dataSourceConfigName.getBytes(StandardCharsets.UTF_8)));

        } catch (final BadCredentialsException | DisabledException | LockedException | AccessDeniedException e) {
            log.warn("Login failed for user '{}': {}", username, e.toString());

            errorMessageTitle.setText(messageBundle.getMessage("loginForm.errorTitle"));
            errorMessageDescription.setText(messageBundle.getMessage("loginForm.badCredentials"));
            errorMessage.setVisible(true);
        }
    }

    @Override
    public void localeChange(final LocaleChangeEvent event) {
        UI.getCurrent().getPage().setTitle(messageBundle.getMessage("LoginView.title"));

        loginFormTitle.setText(messageBundle.getMessage("loginForm.headerTitle"));
        usernameField.setLabel(messageBundle.getMessage("loginForm.username"));
        passwordField.setLabel(messageBundle.getMessage("loginForm.password"));
        rememberMe.setLabel(messageBundle.getMessage("loginForm.rememberMe"));
        submitBtn.setText(messageBundle.getMessage("loginForm.submit"));
    }

    private void initDatabaseConnectionField() {
        List<String> datasourceNames = dataSourceRepository.getDatasourceNames();
        databaseConnectionField.setItems(datasourceNames);
        if (!datasourceNames.isEmpty()) {
            String lastDataSourceConfigName = null;
            String cookieValue = getCookies().getCookieValue(LAST_DATA_SOURCE_CONFIG_NAME_COOKIE);
            if (cookieValue != null) {
                lastDataSourceConfigName = new String(Base64.getDecoder().decode(cookieValue), StandardCharsets.UTF_8);
            }
            if (lastDataSourceConfigName != null && datasourceNames.contains(lastDataSourceConfigName)) {
                databaseConnectionField.setValue(lastDataSourceConfigName);
            } else {
                databaseConnectionField.setValue(datasourceNames.get(0));
            }
        }
    }

    @Subscribe(id = "openDataSourceConfigEntitiesEditorBtn", subject = "clickListener")
    public void onOpenDataSourceConfigEntitiesEditorBtnClick(final ClickEvent<JmixButton> event) {
        dialogWindows.lookup(this, DataSourceConfigEntity.class)
                .withSelectHandler(dataSourceConfigEntities -> {
                    initDatabaseConnectionField();
                    DataSourceConfigEntity datasourceConfigEntity = dataSourceConfigEntities.iterator().next();
                    databaseConnectionField.setValue(datasourceConfigEntity.getName());
                })
                .open();
    }

    protected AppCookies getCookies() {
        if (cookies == null) {
            cookies = new AppCookies();
        }
        return cookies;
    }
}
