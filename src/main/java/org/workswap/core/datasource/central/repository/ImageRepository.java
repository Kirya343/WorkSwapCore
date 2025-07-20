package org.workswap.core.datasource.central.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.workswap.core.datasource.central.model.listingModels.Image;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    // Дополнительные методы, если нужны:
    List<Image> findByListingId(Long listingId);
}