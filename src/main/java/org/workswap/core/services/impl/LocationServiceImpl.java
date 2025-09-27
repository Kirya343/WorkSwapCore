package org.workswap.core.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.LocationDTO;
import org.workswap.common.enums.SearchModelParamType;
import org.workswap.core.services.LocationService;
import org.workswap.core.services.components.ServiceUtils;
import org.workswap.datasource.central.model.listingModels.Location;
import org.workswap.datasource.central.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile({"production", "statistic"})
public class LocationServiceImpl implements LocationService {
    
    private final LocationRepository locationRepository;

    private final ServiceUtils serviceUtils;

    private Location findLocationFromRepostirory(String param, SearchModelParamType paramType) {
        switch (paramType) {
            case ID:
                return locationRepository.findById(Long.parseLong(param)).orElse(null);
            case NAME:
                return locationRepository.findByName(param);
            default:
                throw new IllegalArgumentException("Unknown param type: " + paramType);
        }
    }

    @Override
    public Location findLocation(String param) {
        if (param == null) {
            return null;
        }
        SearchModelParamType paramType = serviceUtils.detectParamType(param);
        return findLocationFromRepostirory(param, paramType);
    }

    public List<Location> getAllDescendants(Location location) {
        List<Location> descendants = new ArrayList<>();
        descendants.add(location);
        if (!location.isCity()) {
            List<Location> cities = locationRepository.findByCountry(location);
            for (Location child : cities) {
                descendants.add(child);
            }
        }
        return descendants;
    }

    @Override
    public List<Location> getCountries() {
        return locationRepository.findByCity(false);
    }

    @Override
    public List<Location> getCities(Long coutryId) {
        return locationRepository.findByCountryId(coutryId);
    }

    @Override
    public void save(Location location) {
        locationRepository.save(location);
    }
    
    @Override
    public Location saveAndReturn(Location location) {
        return locationRepository.save(location);
    }

    public LocationDTO toDTO(Location location) {
        LocationDTO dto = new LocationDTO();
        dto.setCity(location.isCity());
        dto.setCountryId(location.isCity() == true ? location.getCountry().getId() : null);
        dto.setFullName(location.getFullName());
        dto.setName(location.getName());
        dto.setId(location.getId());
        return dto;
    }
}
