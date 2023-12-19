package com.company.sample.security;

import com.company.sample.entity.DataSourceConfigEntity;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "DataSourceConfigEditor", code = DataSourceConfigEditorRole.CODE, scope = "UI")
public interface DataSourceConfigEditorRole {
    String CODE = "data-source-config-editor";

    @ViewPolicy(viewIds = "DataSourceConfigEntity.list")
    void screens();

    @EntityAttributePolicy(entityClass = DataSourceConfigEntity.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = DataSourceConfigEntity.class, actions = EntityPolicyAction.ALL)
    void dataSourceConfigEntity();
}