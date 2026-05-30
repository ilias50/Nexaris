package com.nexaris.orgservice.repositories;

import com.nexaris.orgservice.entities.OrgCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgCatalogEntryRepository extends JpaRepository<OrgCatalogEntry, Integer> {
    List<OrgCatalogEntry> findByCatalogTypeOrderByValueAsc(String catalogType);
    Optional<OrgCatalogEntry> findByCatalogTypeAndValue(String catalogType, String value);
    void deleteByCatalogTypeAndValue(String catalogType, String value);
}
