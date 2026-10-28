package com.daswandh.backend.service;

import com.daswandh.backend.dto.*;
import com.daswandh.backend.entity.Campaign;
import com.daswandh.backend.entity.Milestone;
import com.daswandh.backend.entity.User;
import com.daswandh.backend.enums.CampaignStatus;
import com.daswandh.backend.exception.BadRequestException;
import com.daswandh.backend.exception.ConflictException;
import com.daswandh.backend.exception.ForbiddenException;
import com.daswandh.backend.exception.ResourceNotFoundException;
import com.daswandh.backend.repository.CampaignRepository;
import com.daswandh.backend.repository.MilestoneRepository;
import com.daswandh.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;

    public CampaignService(CampaignRepository campaignRepository,
                           MilestoneRepository milestoneRepository,
                           UserRepository userRepository,
                           LedgerService ledgerService) {
        this.campaignRepository = campaignRepository;
        this.milestoneRepository = milestoneRepository;
        this.userRepository = userRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public CampaignDetailResponse create(String creatorEmail, CreateCampaignRequest request) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateMilestonePlan(request);

        Campaign campaign = campaignRepository.save(new Campaign(creator, request.title().trim(),
                request.description().trim(), request.goalPaise(), request.deadline()));

        List<Milestone> milestones = new ArrayList<>();
        int sequenceNo = 1;
        for (MilestoneRequest item : request.milestones()) {
            milestones.add(new Milestone(campaign, sequenceNo++, item.title().trim(),
                    blankToNull(item.description()), item.amountPaise()));
        }
        milestoneRepository.saveAll(milestones);

        // A brand-new campaign has an empty ledger, so no query is needed.
        return CampaignDetailResponse.from(campaign, milestones, LedgerSummary.of(0, 0));
    }

    @Transactional
    public CampaignDetailResponse publish(String email, Long campaignId) {
        Campaign campaign = campaignRepository.findWithCreatorById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (!campaign.getCreator().getEmail().equals(email)) {
            throw new ForbiddenException("You can only publish your own campaigns");
        }
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new ConflictException("Only draft campaigns can be published");
        }
        if (!campaign.getDeadline().isAfter(LocalDate.now())) {
            throw new BadRequestException("The deadline has already passed");
        }

        campaign.setStatus(CampaignStatus.ACTIVE); // dirty checking writes the UPDATE at commit
        return toDetail(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignDetailResponse getById(Long id, String viewerEmail) {
        Campaign campaign = campaignRepository.findWithCreatorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        boolean isOwner = viewerEmail != null && campaign.getCreator().getEmail().equals(viewerEmail);
        if (campaign.getStatus() == CampaignStatus.DRAFT && !isOwner) {
            throw new ResourceNotFoundException("Campaign not found");
        }
        return toDetail(campaign);
    }

    @Transactional(readOnly = true)
    public PageResponse<CampaignSummaryResponse> listActive(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Campaign> campaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE, pageable);

        // One extra query for the whole page, not one per campaign.
        Map<Long, Long> raised = ledgerService.raisedByCampaign(
                campaigns.getContent().stream().map(Campaign::getId).toList());

        return PageResponse.from(campaigns.map(c ->
                CampaignSummaryResponse.from(c, raised.getOrDefault(c.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public List<CampaignSummaryResponse> listMine(String email) {
        List<Campaign> campaigns = campaignRepository.findByCreatorEmailOrderByCreatedAtDesc(email);
        Map<Long, Long> raised = ledgerService.raisedByCampaign(
                campaigns.stream().map(Campaign::getId).toList());

        return campaigns.stream()
                .map(c -> CampaignSummaryResponse.from(c, raised.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    private void validateMilestonePlan(CreateCampaignRequest request) {
        long total = request.milestones().stream().mapToLong(MilestoneRequest::amountPaise).sum();
        if (total != request.goalPaise()) {
            throw new BadRequestException("Milestone amounts add up to " + total
                    + " paise but the goal is " + request.goalPaise() + " paise");
        }
    }

    private CampaignDetailResponse toDetail(Campaign campaign) {
        List<Milestone> milestones = milestoneRepository.findByCampaignIdOrderBySequenceNoAsc(campaign.getId());
        return CampaignDetailResponse.from(campaign, milestones, ledgerService.getSummary(campaign.getId()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}