package com.daswandh.backend.repository;

import com.daswandh.backend.entity.LedgerEntry;
import com.daswandh.backend.enums.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

// Still extends the bare Repository marker: there is no delete method, so the ledger
// can only be appended to and read.
public interface LedgerEntryRepository extends Repository<LedgerEntry, Long> {

    LedgerEntry save(LedgerEntry entry);

    // Derived from the property path campaign.id
    Page<LedgerEntry> findByCampaignId(Long campaignId, Pageable pageable);

    boolean existsByDonationId(Long donationId);

    // Total of one entry type for one campaign. coalesce turns "no rows" into 0 instead of null.
    @Query("select coalesce(sum(l.amountPaise), 0L) from LedgerEntry l " +
            "where l.campaign.id = :campaignId and l.entryType = :type")
    long sumByCampaignAndType(@Param("campaignId") Long campaignId,
                              @Param("type") LedgerEntryType type);

    // The same total for many campaigns in ONE query (used for list pages)
    @Query("select l.campaign.id as campaignId, sum(l.amountPaise) as totalPaise from LedgerEntry l " +
            "where l.entryType = :type and l.campaign.id in :campaignIds group by l.campaign.id")
    List<CampaignTotal> sumByCampaignIds(@Param("type") LedgerEntryType type,
                                         @Param("campaignIds") Collection<Long> campaignIds);

    // Interface projection: Spring maps the query aliases onto these getters.
    interface CampaignTotal {
        Long getCampaignId();
        Long getTotalPaise();
    }
}