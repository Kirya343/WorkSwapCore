package org.workswap.core.services.mapping.impl;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.datasource.central.model.User;
import org.workswap.datasource.central.model.listingModels.Location;
import org.workswap.datasource.central.model.user.UserSettings;
import org.workswap.common.dto.user.FullUserDTO;
import org.workswap.common.dto.user.UserDTO;
import org.workswap.core.services.mapping.UserMappingService;

@Service
@RequiredArgsConstructor
@Profile("production")
public class UserMappingServiceImpl implements UserMappingService {

    public UserDTO toDto(User user) {

        UserSettings settings = user.getSettings();
                              
        UserDTO dto = new UserDTO(
            user.getId(), 
            user.getSub(), 
            user.getName(), 
            settings.isPhoneVisible() ? user.getPhone() : null, 
            settings.isEmailVisible() ? user.getEmail() : null, 
            user.getBio(), 
            user.getAvatarUrl(),
            user.getLanguages(),
            user.isLocked(), 
            user.isEnabled(), 
            user.getRating(),
            user.getCreatedAt()
        );
        return dto;
    }

    public FullUserDTO toFullDto(User user) {

        Long locationId = Optional
            .ofNullable(user.getLocation())
            .map(Location::getId)
            .orElse(null);
            
        UserSettings settings = user.getSettings();

        FullUserDTO dto = new FullUserDTO(
            user.getId(),
            user.getSub(),
            user.getName(),
            user.getPhone(),
            user.getEmail(),
            user.getBio(),
            user.getAvatarUrl(),
            user.getLanguages(),
            locationId,
            user.isLocked(),
            user.isEnabled(),
            settings.getAvatarType(),
            user.getRating(),
            settings.isTelegramConnected(),
            user.isTermsAccepted(),
            user.getCreatedAt(),
            user.getTermsAcceptanceDate(),
            settings.getGoogleAvatar(),
            settings.getUploadedAvatar(),
            settings.isPhoneVisible(),
            settings.isEmailVisible()
        );

        return dto;
    }
}

