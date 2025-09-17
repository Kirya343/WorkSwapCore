package org.workswap.core.services.command;

import java.util.List;

import org.workswap.common.dto.permission.PermissionDTO;
import org.workswap.datasource.central.model.user.Role;

public interface PermissionCommandSevice {

    void updateRolePermissions(Role role, List<PermissionDTO> permissions);
}   
