package com.daswandh.backend.controller;

import com.daswandh.backend.dto.CampaignDetailResponse;
import com.daswandh.backend.dto.CampaignSummaryResponse;
import com.daswandh.backend.dto.CreateCampaignRequest;
import com.daswandh.backend.dto.PageResponse;
import com.daswandh.backend.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignDetailResponse create(@AuthenticationPrincipal UserDetails principal,
                                         @Valid @RequestBody CreateCampaignRequest request) {
        return campaignService.create(principal.getUsername(), request);
    }

    @PostMapping("/{id}/publish")
    public CampaignDetailResponse publish(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long id) {
        return campaignService.publish(principal.getUsername(), id);
    }

    @GetMapping
    public PageResponse<CampaignSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "12") int size) {
        return campaignService.listActive(page, size);
    }

    // "/mine" is a fixed path, so Spring picks it over "/{id}".
    @GetMapping("/mine")
    public List<CampaignSummaryResponse> mine(@AuthenticationPrincipal UserDetails principal) {
        return campaignService.listMine(principal.getUsername());
    }

    // Public endpoint: principal is null for anonymous visitors.
    @GetMapping("/{id}")
    public CampaignDetailResponse get(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails principal) {
        return campaignService.getById(id, principal == null ? null : principal.getUsername());
    }
}