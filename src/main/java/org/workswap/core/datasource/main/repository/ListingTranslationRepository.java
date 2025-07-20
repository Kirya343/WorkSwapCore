package org.workswap.core.datasource.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.workswap.core.datasource.main.model.Listing;
import org.workswap.core.datasource.main.model.listingModels.ListingTranslation;

@Repository
public interface ListingTranslationRepository extends JpaRepository<ListingTranslation, Long>{
    ListingTranslation findByListingAndLanguage(Listing listing, String lang);
}
