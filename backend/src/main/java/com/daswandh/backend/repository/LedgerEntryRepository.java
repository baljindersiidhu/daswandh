package com.daswandh.backend.repository;

import com.daswandh.backend.entity.LedgerEntry;
import org.springframework.data.repository.Repository;
import java.util.List;

// Extends the bare Repository marker, NOT JpaRepository, so delete()/deleteAll()
// do not exist on this type. Code that tries to delete a ledger entry won't compile.
public interface LedgerEntryRepository extends Repository<LedgerEntry, Long> {

    LedgerEntry save(LedgerEntry entry); // INSERT only, because the entity is immutable

    List<LedgerEntry> findByCampaignIdOrderByIdAsc(Long campaignId);
}