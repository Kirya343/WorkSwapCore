package org.workswap.core.services.mapping.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.permission.PermissionDTO;
import org.workswap.common.dto.permission.RoleDTO;
import org.workswap.core.services.mapping.PermissionMappingService;
import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;

import lombok.RequiredArgsConstructor;

@Service
@Profile("backoffice")
@RequiredArgsConstructor
public class PermissionMappingServiceImpl implements PermissionMappingService{
    
    public PermissionDTO convertPermissionDTO(Permission perm) {
        return new PermissionDTO(perm.getId(), perm.getName(), perm.getComment());
    }

    public RoleDTO convertRoleDTO(Role role) {
        return new RoleDTO(role.getId(), role.getName(), role.getLevel());
    }
}
