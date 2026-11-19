package com.daswandh.backend.service;

import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.entity.Proof;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.CampaignStatus;
import com.daswandh.backend.enums.MilestoneStatus;
import com.daswandh.backend.enums.ProofStatus;
import com.daswandh.backend.enums.Role;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.repository.MilestoneRepository;
import com.daswandh.backend.repository.ProofRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ProofRepository proofRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private UserRepository userRepository;
    @Mock private LedgerService ledgerService;

    @InjectMocks private ReviewService service;

    private final User admin = new User("admin@test.com", "hash", "Admin", Role.ADMIN);
    private final User creator = new User("creator@test.com", "hash", "Creator", Role.CREATOR);

    private Campaign campaign() {
        Campaign campaign = new Campaign(creator, "Flood relief", "Description text", 500_000L,
                LocalDate.now().plusDays(30));
        campaign.setStatus(CampaignStatus.ACTIVE);
        ReflectionTestUtils.setField(campaign, "id", 1L);
        return campaign;
    }

    private Milestone milestone(Campaign campaign, int sequenceNo, MilestoneStatus status) {
        Milestone milestone = new Milestone(campaign, sequenceNo, "Milestone " + sequenceNo, null, 200_000L);
        milestone.setStatus(status);
        ReflectionTestUtils.setField(milestone, "id", 10L + sequenceNo);
        return milestone;
    }

    private Proof pendingProof(Milestone milestone) {
        Proof proof = new Proof(milestone, creator, "https://res.cloudinary.com/x/p.png", "hash", "desc", 150_000L);
        ReflectionTestUtils.setField(proof, "id", 1L);
        return proof;
    }

    @Test
    void approvingAProofVerifiesTheMilestone() {
        Milestone milestone = milestone(campaign(), 1, MilestoneStatus.PROOF_SUBMITTED);
        Proof proof = pendingProof(milestone);
        when(proofRepository.findById(1L)).thenReturn(Optional.of(proof));
        when(milestoneRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(milestone));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        service.approve("admin@test.com", 1L);

        assertThat(proof.getStatus()).isEqualTo(ProofStatus.APPROVED);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.VERIFIED);
    }

    @Test
    void aProofThatWasAlreadyReviewedCannotBeReviewedAgain() {
        Milestone milestone = milestone(campaign(), 1, MilestoneStatus.VERIFIED);
        when(proofRepository.findById(1L)).thenReturn(Optional.of(pendingProof(milestone)));
        when(milestoneRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(milestone));

        assertThatThrownBy(() -> service.approve("admin@test.com", 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been reviewed");
    }

    @Test
    void rejectingSendsTheMilestoneBackSoTheCreatorCanUploadAgain() {
        Milestone milestone = milestone(campaign(), 1, MilestoneStatus.PROOF_SUBMITTED);
        Proof proof = pendingProof(milestone);
        when(proofRepository.findById(1L)).thenReturn(Optional.of(proof));
        when(milestoneRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(milestone));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        service.reject("admin@test.com", 1L, "  Receipt is unreadable  ");

        assertThat(proof.getStatus()).isEqualTo(ProofStatus.REJECTED);
        assertThat(proof.getReviewNote()).isEqualTo("Receipt is unreadable");
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.RELEASED);
    }

    @Test
    void releasingTheFirstMilestoneRecordsTheLedgerEntryAndMarksItReleased() {
        Milestone milestone = milestone(campaign(), 1, MilestoneStatus.LOCKED);
        when(milestoneRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(milestone));

        service.release("admin@test.com", 11L);

        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.RELEASED);
        verify(ledgerService).recordMilestoneRelease(milestone);
    }

    @Test
    void aMilestoneCannotBeReleasedBeforeThePreviousOneIsVerified() {
        Campaign campaign = campaign();
        Milestone first = milestone(campaign, 1, MilestoneStatus.RELEASED); // spent, proof not verified yet
        Milestone second = milestone(campaign, 2, MilestoneStatus.LOCKED);
        when(milestoneRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(second));
        when(milestoneRepository.findByCampaignIdAndSequenceNo(1L, 1)).thenReturn(Optional.of(first));

        assertThatThrownBy(() -> service.release("admin@test.com", 12L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("must be verified");
        verify(ledgerService, never()).recordMilestoneRelease(any(Milestone.class));
    }

    @Test
    void anAlreadyReleasedMilestoneCannotBeReleasedTwice() {
        Milestone milestone = milestone(campaign(), 1, MilestoneStatus.RELEASED);
        when(milestoneRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(milestone));

        assertThatThrownBy(() -> service.release("admin@test.com", 11L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been released");
        verify(ledgerService, never()).recordMilestoneRelease(any(Milestone.class));
    }
}