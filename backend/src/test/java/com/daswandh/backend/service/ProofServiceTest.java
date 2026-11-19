package com.daswandh.backend.service;

import com.daswandh.backend.dto.ProofResponse;
import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.entity.Proof;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.MilestoneStatus;
import com.daswandh.backend.enums.ProofStatus;
import com.daswandh.backend.enums.Role;
import com.daswandh.backend.exception.BadRequestException;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ForbiddenException;
import com.daswandh.backend.repository.MilestoneRepository;
import com.daswandh.backend.repository.ProofRepository;
import com.daswandh.backend.repository.UserRepository;
import com.daswandh.backend.storage.CloudinaryClient;
import com.daswandh.backend.storage.ImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProofServiceTest {

    @Mock private MilestoneRepository milestoneRepository;
    @Mock private ProofRepository proofRepository;
    @Mock private UserRepository userRepository;
    @Mock private CloudinaryClient cloudinaryClient;

    private ProofService proofService;
    private final User creator = new User("creator@test.com", "hash", "Creator", Role.CREATOR);

    @BeforeEach
    void setUp() {
        proofService = new ProofService(milestoneRepository, proofRepository, userRepository,
                new ImageValidator(), cloudinaryClient);
    }

    private Milestone milestone(MilestoneStatus status) {
        Campaign campaign = new Campaign(creator, "Flood relief", "Description text", 500_000L,
                LocalDate.now().plusDays(30));
        Milestone milestone = new Milestone(campaign, 1, "Buy tents", null, 200_000L);
        milestone.setStatus(status);
        ReflectionTestUtils.setField(milestone, "id", 10L);
        return milestone;
    }

    private MockMultipartFile png() {
        byte[] bytes = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
        return new MockMultipartFile("file", "receipt.png", "image/png", bytes);
    }

    @Test
    void validSubmissionUploadsSavesAPendingProofAndMovesTheMilestone() {
        Milestone milestone = milestone(MilestoneStatus.RELEASED);
        when(milestoneRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(milestone));
        when(userRepository.findByEmail("creator@test.com")).thenReturn(Optional.of(creator));
        when(cloudinaryClient.uploadImage(any(byte[].class), anyString()))
                .thenReturn(new CloudinaryClient.UploadedFile("https://res.cloudinary.com/x/proof.png", "id"));
        when(proofRepository.save(any(Proof.class))).thenAnswer(call -> call.getArgument(0));

        ProofResponse response = proofService.submitProof("creator@test.com", 10L, png(), "Bought tents", 150_000L);

        assertThat(response.status()).isEqualTo(ProofStatus.PENDING);
        assertThat(response.fileUrl()).isEqualTo("https://res.cloudinary.com/x/proof.png");
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.PROOF_SUBMITTED);
    }

    @Test
    void anotherCreatorCannotSubmitAndNothingIsUploaded() {
        when(milestoneRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(milestone(MilestoneStatus.RELEASED)));

        assertThatThrownBy(() -> proofService.submitProof("intruder@test.com", 10L, png(), "desc", 100L))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(cloudinaryClient);
    }

    @Test
    void milestoneThatIsNotReleasedIsRejected() {
        when(milestoneRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(milestone(MilestoneStatus.LOCKED)));

        assertThatThrownBy(() -> proofService.submitProof("creator@test.com", 10L, png(), "desc", 100L))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(cloudinaryClient);
    }

    @Test
    void aNonImageFileIsRejectedEvenIfItClaimsToBeAPng() {
        MockMultipartFile fake = new MockMultipartFile("file", "receipt.png", "image/png",
                "just some text".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> proofService.submitProof("creator@test.com", 10L, fake, "desc", 100L))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(milestoneRepository, cloudinaryClient);
    }
}