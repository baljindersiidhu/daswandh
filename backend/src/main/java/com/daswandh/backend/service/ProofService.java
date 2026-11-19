package com.daswandh.backend.service;

import com.daswandh.backend.dto.ProofResponse;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.entity.Proof;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.CampaignStatus;
import com.daswandh.backend.enums.MilestoneStatus;
import com.daswandh.backend.exception.BadRequestException;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ForbiddenException;
import com.daswandh.backend.exception.ResourceNotFoundException;
import com.daswandh.backend.repository.MilestoneRepository;
import com.daswandh.backend.repository.ProofRepository;
import com.daswandh.backend.repository.UserRepository;
import com.daswandh.backend.storage.CloudinaryClient;
import com.daswandh.backend.storage.Hashing;
import com.daswandh.backend.storage.ImageValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProofService {

    private final MilestoneRepository milestoneRepository;
    private final ProofRepository proofRepository;
    private final UserRepository userRepository;
    private final ImageValidator imageValidator;
    private final CloudinaryClient cloudinaryClient;

    public ProofService(MilestoneRepository milestoneRepository,
                        ProofRepository proofRepository,
                        UserRepository userRepository,
                        ImageValidator imageValidator,
                        CloudinaryClient cloudinaryClient) {
        this.milestoneRepository = milestoneRepository;
        this.proofRepository = proofRepository;
        this.userRepository = userRepository;
        this.imageValidator = imageValidator;
        this.cloudinaryClient = cloudinaryClient;
    }

    @Transactional
    public ProofResponse submitProof(String creatorEmail, Long milestoneId, MultipartFile file,
                                     String description, long amountSpentPaise) {
        // Cheap checks first
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.isEmpty() || cleanDescription.length() > 1000) {
            throw new BadRequestException("Description is required (max 1000 characters)");
        }
        if (amountSpentPaise <= 0) {
            throw new BadRequestException("Amount spent must be greater than 0");
        }
        byte[] bytes = readBytes(file);
        ImageValidator.DetectedImage image = imageValidator.validate(bytes); // checks the REAL file type and size

        // Lock the milestone, so two uploads at once can't both pass the checks below
        Milestone milestone = milestoneRepository.findByIdForUpdate(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        if (!milestone.getCampaign().getCreator().getEmail().equals(creatorEmail)) {
            throw new ForbiddenException("You can only submit proofs for your own campaigns");
        }
        if (milestone.getStatus() != MilestoneStatus.RELEASED) {
            throw new ConflictException("Proof can only be submitted for a released milestone (current status: "
                    + milestone.getStatus() + ")");
        }
        if (amountSpentPaise > milestone.getAmountPaise()) {
            throw new BadRequestException("Amount spent cannot be more than the milestone amount of "
                    + milestone.getAmountPaise() + " paise");
        }

        User uploader = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CloudinaryClient.UploadedFile uploaded = cloudinaryClient.uploadImage(bytes, "proof." + image.extension());

        // The hash is the file's fingerprint. Step 10 uses it to spot reused receipts.
        Proof proof = proofRepository.save(new Proof(milestone, uploader, uploaded.url(),
                Hashing.sha256Hex(bytes), cleanDescription, amountSpentPaise));
        milestone.setStatus(MilestoneStatus.PROOF_SUBMITTED);
        return ProofResponse.from(proof);
    }

    // Public: anyone can see the proofs of a published campaign's milestone
    @Transactional(readOnly = true)
    public List<ProofResponse> listForMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        if (milestone.getCampaign().getStatus() == CampaignStatus.DRAFT) {
            throw new ResourceNotFoundException("Milestone not found");
        }
        return proofRepository.findByMilestoneIdOrderByCreatedAtDesc(milestoneId).stream()
                .map(ProofResponse::from)
                .toList();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }
    }
}