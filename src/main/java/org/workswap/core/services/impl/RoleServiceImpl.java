package org.workswap.core.services.impl;

import org.springframework.stereotype.Service;
import org.workswap.core.services.RoleService;
import org.workswap.datasource.central.model.user.Role;
import org.workswap.datasource.central.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name);
    }
    
}
