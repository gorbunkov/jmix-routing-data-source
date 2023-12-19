# Switch Data Source on Login Sample

## Overview

Database connection parameters are stored in a database table. See `DataStoreConfigEntity`.

The main datastore must be defined.

```properties
main.datasource.url = jdbc:postgresql://localhost/routingdatasource
main.datasource.username = root
main.datasource.password =root
```

It serves for the following purposes:

1. This datastore is used when it impossible to determine the datasource name from the session attribute (e.g. from a background task).
2. It defines DBMS type (postgres, oracle, etc.) for all custom data sources defined on the login view.
3. It stores `DataStoreConfigEntity` records.

`DataSourceRepository` stores DataSource instances for all defined database connections.

`MyRoutingDatasource` class is the DataSource implementation that returns a connection for a DataSource whose name is stored in HTTP session.

`DataSourceConfigEntityEventListener` updates the `DataSourceRepository` on each `DataSourceConfigEntity` modification.

`DataSourceConfigEditorRole` must be granted to anonymous user to edit entities with connection parameters from the login view (see `DatabaseUserRepository`).

`LoginView` screen is modified, it uses custom components instead of LoginForm.