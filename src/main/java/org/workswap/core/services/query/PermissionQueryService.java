package org.workswap.core.services.query;

import java.util.List;

import org.workswap.common.dto.permission.PermissionDTO;
import org.workswap.common.dto.permission.RoleDTO;
import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;

public interface PermissionQueryService {
    
    Role findRole(String param);
    Permission findPermission(String param);

    List<RoleDTO> getAllRoleDtos();
    List<PermissionDTO> getAllPermissionDtos();
    List<PermissionDTO> getPermissionDtosByRole(Role role);
}
