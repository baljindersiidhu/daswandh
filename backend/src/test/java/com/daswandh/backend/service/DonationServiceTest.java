package com.daswandh.backend.service;

import com.daswandh.backend.dto.CreateDonationRequest;
import com.daswandh.backend.dto.DonationOrderResponse;
import com.daswandh.backend.dto.DonationResponse;
import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Donation;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.CampaignStatus;
import com.daswandh.backend.enums.DonationStatus;
import com.daswandh.backend.enums.Role;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ForbiddenException;
import com.daswandh.backend.exception.ResourceNotFoundException;
import com.daswandh.backend.payment.RazorpayClient;
import com.daswandh.backend.repository.CampaignRepository;
import com.daswandh.backend.repository.DonationRepository;
import com.daswandh.backend.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock private DonationRepository donationRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private UserRepository userRepository;
    @Mock private LedgerService ledgerService;
    @Mock private RazorpayClient razorpayClient;

    @InjectMocks private DonationService donationService;

    // ---------- helpers ----------

    private User donor(String email) {
        return new User(email, "hash", "Donor", Role.DONOR);
    }

    private Campaign campaign(CampaignStatus status) {
        User creator = new User("creator@test.com", "hash", "Creator", Role.CREATOR);
        Campaign campaign = new Campaign(creator, "Flood relief", "Description text",
                500_000L, LocalDate.now().plusDays(30));
        campaign.setStatus(status);
        ReflectionTestUtils.setField(campaign, "id", 1L);
        return campaign;
    }

    private Donation donation(String donorEmail, DonationStatus status) {
        Donation donation = new Donation(campaign(CampaignStatus.ACTIVE), donor(donorEmail), 50_000L, false, "order_1");
        donation.setStatus(status);
        ReflectionTestUtils.setField(donation, "id", 5L);
        return donation;
    }

    // ---------- confirming a payment ----------

    @Test
    void confirmMarksDonationPaidAndRecordsItInTheLedger() {
        Donation donation = donation("donor@test.com", DonationStatus.CREATED);
        when(donationRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(donation));

        DonationResponse response = donationService.confirmPaymentAsDonor("donor@test.com", "order_1", "pay_1");

        assertThat(donation.getStatus()).isEqualTo(DonationStatus.PAID);
        assertThat(donation.getRazorpayPaymentId()).isEqualTo("pay_1");
        assertThat(response.status()).isEqualTo(DonationStatus.PAID);
        verify(ledgerService).recordDonation(donation);
    }

    @Test
    void confirmingTwiceDoesNotRecordTheLedgerTwice() {
        Donation donation = donation("donor@test.com", DonationStatus.PAID);
        when(donationRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(donation));

        donationService.confirmPaymentAsDonor("donor@test.com", "order_1", "pay_1");

        verify(ledgerService, never()).recordDonation(any(Donation.class));
    }

    @Test
    void anotherDonorCannotConfirmSomeoneElsesDonation() {
        Donation donation = donation("donor@test.com", DonationStatus.CREATED);
        when(donationRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(donation));

        assertThatThrownBy(() -> donationService.confirmPaymentAsDonor("intruder@test.com", "order_1", "pay_1"))
                .isInstanceOf(ForbiddenException.class);
        assertThat(donation.getStatus()).isEqualTo(DonationStatus.CREATED);
        verify(ledgerService, never()).recordDonation(any(Donation.class));
    }

    @Test
    void unknownOrderIsNotFound() {
        when(donationRepository.findByRazorpayOrderIdForUpdate("order_x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> donationService.confirmPaymentAsDonor("donor@test.com", "order_x", "pay_1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- creating an order ----------

    @Test
    void orderIsRejectedForACampaignThatIsNotActive() {
        when(userRepository.findByEmail("donor@test.com")).thenReturn(Optional.of(donor("donor@test.com")));
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign(CampaignStatus.DRAFT)));

        assertThatThrownBy(() -> donationService.createOrder("donor@test.com", 1L,
                new CreateDonationRequest(50_000L, false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not accepting donations");
        verify(razorpayClient, never()).createOrder(anyLong(), anyString(), anyMap());
    }

    @Test
    void orderIsCreatedAndSavedForAnActiveCampaign() {
        when(userRepository.findByEmail("donor@test.com")).thenReturn(Optional.of(donor("donor@test.com")));
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign(CampaignStatus.ACTIVE)));
        when(razorpayClient.createOrder(anyLong(), anyString(), anyMap())).thenReturn("order_abc");
        when(razorpayClient.getKeyId()).thenReturn("rzp_test_key");
        when(donationRepository.save(any(Donation.class))).thenAnswer(call -> call.getArgument(0));

        DonationOrderResponse response = donationService.createOrder("donor@test.com", 1L,
                new CreateDonationRequest(50_000L, true));

        assertThat(response.razorpayOrderId()).isEqualTo("order_abc");
        assertThat(response.amountPaise()).isEqualTo(50_000L);
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.razorpayKeyId()).isEqualTo("rzp_test_key");
    }
}