package org.workswap.core.services.mapping;

import org.workswap.common.dto.permission.PermissionDTO;
import org.workswap.common.dto.permission.RoleDTO;
import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;

public interface PermissionMappingService {
    
    PermissionDTO convertPermissionDTO(Permission perm);
    RoleDTO convertRoleDTO(Role role);
}
