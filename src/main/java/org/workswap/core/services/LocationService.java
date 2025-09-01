package org.workswap.core.services;

import java.util.List;

import org.workswap.common.dto.LocationDTO;
import org.workswap.datasource.central.model.listingModels.Location;

public interface LocationService {
    
    List<Location> getAllDescendants(Location location);

    List<Location> getCities(Long coutryId);
    List<Location> getCountries();

    LocationDTO toDTO(Location location);
}
