package org.workswap.core.services.command.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.permission.PermissionDTO;
import org.workswap.core.services.command.PermissionCommandSevice;
import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;
import org.workswap.datasource.central.repository.PermissionRepository;
import org.workswap.datasource.central.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionCommandSeviceImpl implements PermissionCommandSevice {

    private static final Logger logger = LoggerFactory.getLogger(PermissionCommandSevice.class);
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public void updateRolePermissions(Role role, List<PermissionDTO> permissions) {
        
        List<Long> ids = permissions.stream().map(perm -> perm.getId()).toList();
        logger.debug("Найденые id пермишнов: {}", ids);

        Set<Permission> newPerms = permissionRepository
                .findAllByIdIn(ids)
                .stream()
                .collect(Collectors.toSet());

        logger.debug("Найденые новые пермишны: {}", newPerms);

        role.setPermissions(newPerms);
        roleRepository.save(role);
    }
}
