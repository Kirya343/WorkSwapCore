package org.workswap.core.services.command.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.workswap.core.services.command.PermissionCommandSevice;
import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;
import org.workswap.datasource.central.repository.PermissionRepository;
import org.workswap.datasource.central.repository.RoleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionCommandSeviceImpl implements PermissionCommandSevice {

    private static final Logger logger = LoggerFactory.getLogger(PermissionCommandSevice.class);
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public void updateRolePermission(Role role, Permission permission, boolean enabled) {
        
        if (enabled == true) {
            logger.debug("Удаляем пермишн {} у роли {}", permission.getName(), role.getName());
            role.getPermissions().add(permission);
        } else {
            logger.debug("Добавляем пермишн {} у роли {}", permission.getName(), role.getName());
            role.getPermissions().remove(permission);
        }

        saveRole(role);
    }

    @Transactional
    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    public Permission savePermission(Permission permission) {
        return permissionRepository.save(permission);
    }
}
