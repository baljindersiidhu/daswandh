package com.daswandh.backend.service;

import com.daswandh.backend.dto.LedgerEntryResponse;
import com.daswandh.backend.dto.LedgerResponse;
import com.daswandh.backend.dto.LedgerSummary;
import com.daswandh.backend.dto.PageResponse;
import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Donation;
import com.daswandh.backend.entity.LedgerEntry;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.enums.CampaignStatus;
import com.daswandh.backend.enums.DonationStatus;
import com.daswandh.backend.enums.LedgerEntryType;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ResourceNotFoundException;
import com.daswandh.backend.repository.CampaignRepository;
import com.daswandh.backend.repository.LedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// The ONLY class that writes to the ledger.
@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final CampaignRepository campaignRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, CampaignRepository campaignRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.campaignRepository = campaignRepository;
    }

    // ===================== WRITES =====================
    // MANDATORY: the caller must already be in a transaction (e.g. "mark donation PAID + record it").
    // If not, Spring throws, so a ledger entry can never be saved on its own by accident.

    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry recordDonation(Donation donation) {
        if (donation.getStatus() != DonationStatus.PAID) {
            throw new ConflictException("Only paid donations can be recorded in the ledger");
        }
        // Friendly check. The unique index in the database is the real guarantee.
        if (ledgerEntryRepository.existsByDonationId(donation.getId())) {
            throw new ConflictException("Donation is already recorded in the ledger");
        }
        return ledgerEntryRepository.save(LedgerEntry.donationIn(donation));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry recordMilestoneRelease(Milestone milestone) {
        Long campaignId = milestone.getCampaign().getId();

        // Lock this campaign's row until the transaction ends. A second release for the same
        // campaign waits here, then sees the balance AFTER our entry, so we can't overspend.
        campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        long available = calculateSummary(campaignId).availablePaise();
        if (milestone.getAmountPaise() > available) {
            throw new ConflictException("Not enough funds: " + available + " paise available, but the milestone needs "
                    + milestone.getAmountPaise() + " paise");
        }
        return ledgerEntryRepository.save(LedgerEntry.milestoneRelease(milestone));
    }

    // ===================== READS =====================

    @Transactional(readOnly = true)
    public LedgerSummary getSummary(Long campaignId) {
        return calculateSummary(campaignId);
    }

    // Raised amount for many campaigns with a single query. Campaigns with no donations are absent from the map.
    @Transactional(readOnly = true)
    public Map<Long, Long> raisedByCampaign(Collection<Long> campaignIds) {
        Map<Long, Long> result = new HashMap<>();
        if (campaignIds.isEmpty()) {
            return result;
        }
        ledgerEntryRepository.sumByCampaignIds(LedgerEntryType.DONATION_IN, campaignIds)
                .forEach(row -> result.put(row.getCampaignId(), row.getTotalPaise()));
        return result;
    }

    @Transactional(readOnly = true)
    public LedgerResponse getPublicLedger(Long campaignId, int page, int size) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        if (campaign.getStatus() == CampaignStatus.DRAFT) {
            throw new ResourceNotFoundException("Campaign not found");
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id")); // newest first
        Page<LedgerEntryResponse> entries = ledgerEntryRepository.findByCampaignId(campaignId, pageable)
                .map(LedgerEntryResponse::from);

        return new LedgerResponse(calculateSummary(campaignId), PageResponse.from(entries));
    }

    // The balance is never stored: it is always calculated from the rows.
    private LedgerSummary calculateSummary(Long campaignId) {
        long raised = ledgerEntryRepository.sumByCampaignAndType(campaignId, LedgerEntryType.DONATION_IN);
        long released = ledgerEntryRepository.sumByCampaignAndType(campaignId, LedgerEntryType.MILESTONE_RELEASE_OUT);
        return LedgerSummary.of(raised, released);
    }
}