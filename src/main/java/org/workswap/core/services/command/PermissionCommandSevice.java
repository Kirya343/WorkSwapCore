package org.workswap.core.services.command;

import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;

public interface PermissionCommandSevice {

    void updateRolePermission(Role role, Permission permission, boolean enabled);

    Role saveRole(Role role);
    Permission savePermission(Permission permission);
}   
