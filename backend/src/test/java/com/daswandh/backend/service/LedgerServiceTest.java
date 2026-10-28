package com.daswandh.backend.service;

import com.daswandh.backend.dto.LedgerSummary;
import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Donation;
import com.daswandh.backend.entity.LedgerEntry;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.DonationStatus;
import com.daswandh.backend.enums.LedgerEntryType;
import com.daswandh.backend.enums.Role;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.repository.CampaignRepository;
import com.daswandh.backend.repository.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// A pure unit test: no Spring, no database. The repositories are mocks.
@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private LedgerService ledgerService;

    // ---------- helpers: ids are normally set by the database, so we set them by reflection ----------

    private Campaign campaign() {
        User creator = new User("creator@test.com", "hash", "Creator", Role.CREATOR);
        Campaign campaign = new Campaign(creator, "Flood relief", "Description text",
                500_000L, LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(campaign, "id", 1L);
        return campaign;
    }

    private Milestone milestone(Campaign campaign, long amountPaise) {
        Milestone milestone = new Milestone(campaign, 1, "Buy tents", null, amountPaise);
        ReflectionTestUtils.setField(milestone, "id", 10L);
        return milestone;
    }

    private Donation donation(Campaign campaign, DonationStatus status) {
        User donor = new User("donor@test.com", "hash", "Donor", Role.DONOR);
        Donation donation = new Donation(campaign, donor, 50_000L, false, "order_1");
        donation.setStatus(status);
        ReflectionTestUtils.setField(donation, "id", 5L);
        return donation;
    }

    // ---------- milestone release ----------

    @Test
    void releaseSavesLedgerEntryWhenFundsAreAvailable() {
        Campaign campaign = campaign();
        Milestone milestone = milestone(campaign, 200_000L);
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.DONATION_IN)).thenReturn(500_000L);
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.MILESTONE_RELEASE_OUT)).thenReturn(0L);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(call -> call.getArgument(0));

        LedgerEntry entry = ledgerService.recordMilestoneRelease(milestone);

        assertThat(entry.getEntryType()).isEqualTo(LedgerEntryType.MILESTONE_RELEASE_OUT);
        assertThat(entry.getAmountPaise()).isEqualTo(200_000L);
    }

    @Test
    void releaseIsRejectedWhenFundsAreInsufficient() {
        Campaign campaign = campaign();
        Milestone milestone = milestone(campaign, 200_000L);
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.DONATION_IN)).thenReturn(100_000L);
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.MILESTONE_RELEASE_OUT)).thenReturn(0L);

        assertThatThrownBy(() -> ledgerService.recordMilestoneRelease(milestone))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Not enough funds");
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void releaseCountsMoneyAlreadyReleased() {
        Campaign campaign = campaign();
        Milestone milestone = milestone(campaign, 200_000L);
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        // 300k raised, 200k already released: only 100k is left, so another 200k must fail
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.DONATION_IN)).thenReturn(300_000L);
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.MILESTONE_RELEASE_OUT)).thenReturn(200_000L);

        assertThatThrownBy(() -> ledgerService.recordMilestoneRelease(milestone))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- donations ----------

    @Test
    void donationIsRecordedWhenPaid() {
        Donation donation = donation(campaign(), DonationStatus.PAID);
        when(ledgerEntryRepository.existsByDonationId(5L)).thenReturn(false);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(call -> call.getArgument(0));

        LedgerEntry entry = ledgerService.recordDonation(donation);

        assertThat(entry.getEntryType()).isEqualTo(LedgerEntryType.DONATION_IN);
        assertThat(entry.getAmountPaise()).isEqualTo(50_000L);
    }

    @Test
    void unpaidDonationIsRejected() {
        Donation donation = donation(campaign(), DonationStatus.CREATED);

        assertThatThrownBy(() -> ledgerService.recordDonation(donation))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only paid donations");
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    void sameDonationCannotBeRecordedTwice() {
        Donation donation = donation(campaign(), DonationStatus.PAID);
        when(ledgerEntryRepository.existsByDonationId(5L)).thenReturn(true);

        assertThatThrownBy(() -> ledgerService.recordDonation(donation))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already recorded");
        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
    }

    // ---------- balances ----------

    @Test
    void availableIsRaisedMinusReleased() {
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.DONATION_IN)).thenReturn(500_000L);
        when(ledgerEntryRepository.sumByCampaignAndType(1L, LedgerEntryType.MILESTONE_RELEASE_OUT)).thenReturn(200_000L);

        LedgerSummary summary = ledgerService.getSummary(1L);

        assertThat(summary.raisedPaise()).isEqualTo(500_000L);
        assertThat(summary.releasedPaise()).isEqualTo(200_000L);
        assertThat(summary.availablePaise()).isEqualTo(300_000L);
    }
}