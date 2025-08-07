package org.workswap.core.services;

import org.workswap.datasource.central.model.user.Role;

public interface RoleService {

    Role getRoleByName(String name);
    
}
