package org.workswap.core.services.command;

import java.nio.file.AccessDeniedException;
import java.util.Map;

import org.workswap.datasource.central.model.Listing;
import org.workswap.datasource.central.model.User;

public interface ListingCommandService {

    void save(Listing listing);
    Listing saveAndReturn(Listing listing);

    void delete(Listing listing);
    void clearStatSnapshots(Listing listing);

    void toggleFavorite(User user, Listing listing);
    void addListingToFavorite(User user, Listing listing);
    void removeListingFromFavorite(User user, Listing listing);

    void modifyListingParam(User user, Long id, Map<String, Object> updates) throws AccessDeniedException;
}
