package com.daswandh.backend.service;

import com.daswandh.backend.dto.MilestoneResponse;
import com.daswandh.backend.dto.ProofResponse;
import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.entity.Proof;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.CampaignStatus;
import com.daswandh.backend.enums.MilestoneStatus;
import com.daswandh.backend.enums.ProofStatus;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ResourceNotFoundException;
import com.daswandh.backend.repository.MilestoneRepository;
import com.daswandh.backend.repository.ProofRepository;
import com.daswandh.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ProofRepository proofRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;

    public ReviewService(ProofRepository proofRepository,
                         MilestoneRepository milestoneRepository,
                         UserRepository userRepository,
                         LedgerService ledgerService) {
        this.proofRepository = proofRepository;
        this.milestoneRepository = milestoneRepository;
        this.userRepository = userRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional(readOnly = true)
    public List<ProofResponse> pendingProofs() {
        return proofRepository.findByStatusOrderByCreatedAtAsc(ProofStatus.PENDING).stream()
                .map(ProofResponse::from)
                .toList();
    }

    @Transactional
    public ProofResponse approve(String adminEmail, Long proofId) {
        Proof proof = findProof(proofId);
        Milestone milestone = lockReviewableMilestone(proof);

        proof.review(ProofStatus.APPROVED, findUser(adminEmail), null);
        milestone.setStatus(MilestoneStatus.VERIFIED);
        log.info("Admin {} approved proof {}", adminEmail, proofId);
        return ProofResponse.from(proof);
    }

    @Transactional
    public ProofResponse reject(String adminEmail, Long proofId, String note) {
        Proof proof = findProof(proofId);
        Milestone milestone = lockReviewableMilestone(proof);

        proof.review(ProofStatus.REJECTED, findUser(adminEmail), note.trim());
        milestone.setStatus(MilestoneStatus.RELEASED); // back to "creator must upload again"
        log.info("Admin {} rejected proof {}", adminEmail, proofId);
        return ProofResponse.from(proof);
    }

    // The core rule: money for milestone N moves only after milestone N-1 is VERIFIED.
    @Transactional
    public MilestoneResponse release(String adminEmail, Long milestoneId) {
        Milestone milestone = milestoneRepository.findByIdForUpdate(milestoneId) // lock first
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        Campaign campaign = milestone.getCampaign();

        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new ConflictException("The campaign is not active");
        }
        if (milestone.getStatus() != MilestoneStatus.LOCKED) {
            throw new ConflictException("This milestone has already been released");
        }
        if (milestone.getSequenceNo() > 1) {
            Milestone previous = milestoneRepository
                    .findByCampaignIdAndSequenceNo(campaign.getId(), milestone.getSequenceNo() - 1)
                    .orElseThrow(() -> new IllegalStateException("Milestone sequence is broken"));
            if (previous.getStatus() != MilestoneStatus.VERIFIED) {
                throw new ConflictException("Milestone " + previous.getSequenceNo()
                        + " must be verified before releasing milestone " + milestone.getSequenceNo());
            }
        }

        // Checks the balance and appends the ledger entry in THIS transaction
        ledgerService.recordMilestoneRelease(milestone);
        milestone.setStatus(MilestoneStatus.RELEASED);
        log.info("Admin {} released milestone {} of campaign {}", adminEmail, milestoneId, campaign.getId());
        return MilestoneResponse.from(milestone);
    }

    private Proof findProof(Long proofId) {
        return proofRepository.findById(proofId)
                .orElseThrow(() -> new ResourceNotFoundException("Proof not found"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Locks the milestone, then confirms the proof is still waiting for review. The milestone is
    // loaded AFTER the lock, so its status is fresh even if another admin acted a moment ago.
    private Milestone lockReviewableMilestone(Proof proof) {
        Milestone milestone = milestoneRepository.findByIdForUpdate(proof.getMilestone().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
        if (proof.getStatus() != ProofStatus.PENDING || milestone.getStatus() != MilestoneStatus.PROOF_SUBMITTED) {
            throw new ConflictException("This proof has already been reviewed");
        }
        return milestone;
    }
}