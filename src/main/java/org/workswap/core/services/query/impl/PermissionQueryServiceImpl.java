package org.workswap.core.services.query.impl;

import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.permission.PermissionDTO;
import org.workswap.common.dto.permission.RoleDTO;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.core.services.components.ServiceUtils;
import org.workswap.core.services.mapping.PermissionMappingService;
import org.workswap.core.services.query.PermissionQueryService;
import org.workswap.datasource.central.model.user.Permission;
import org.workswap.datasource.central.model.user.Role;
import org.workswap.datasource.central.repository.PermissionRepository;
import org.workswap.datasource.central.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"production", "statistic"})
@RequiredArgsConstructor
public class PermissionQueryServiceImpl implements PermissionQueryService {
    
    private final ServiceUtils serviceUtils;
    private final PermissionMappingService mappingService;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private Role findRoleFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return roleRepository.findById(Long.parseLong(param)).orElse(null);
            case NAME:
                return roleRepository.findByName(param);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    private Permission findPermissionFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return permissionRepository.findById(Long.parseLong(param)).orElse(null);
            case NAME:
                return permissionRepository.findByName(param);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    public Role findRole(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findRoleFromRepostirory(param, paramType);
    }

    public Permission findPermission(String param) {
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findPermissionFromRepostirory(param, paramType);
    }

    public List<RoleDTO> getAllRoleDtos() {
        List<Role> roles = roleRepository.findAll();

        List<RoleDTO> dtos = roles.stream().map(role -> mappingService.convertRoleDTO(role)).toList();;
        return dtos;
    }

    public List<PermissionDTO> getAllPermissionDtos() {
        List<Permission> perms = permissionRepository.findAll();

        List<PermissionDTO> dtos = perms.stream().map(perm -> mappingService.convertPermissionDTO(perm)).toList();
        return dtos;
    }

    public List<PermissionDTO> getPermissionDtosByRole(Role role) {
        Set<Permission> roles = role.getPermissions();
        
        List<PermissionDTO> dtos = roles.stream().map(perm -> mappingService.convertPermissionDTO(perm)).toList();
        return dtos;
    }
}
