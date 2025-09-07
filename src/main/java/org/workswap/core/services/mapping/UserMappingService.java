package org.workswap.core.services.mapping;

import org.workswap.common.dto.user.FullUserDTO;
import org.workswap.common.dto.user.UserDTO;
import org.workswap.datasource.central.model.User;

public interface UserMappingService {

    UserDTO toDto(User user);

    FullUserDTO toFullDto(User user);
}
