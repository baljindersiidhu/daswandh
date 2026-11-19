package com.daswandh.backend.controller;

import com.daswandh.backend.dto.MilestoneResponse;
import com.daswandh.backend.dto.ProofResponse;
import com.daswandh.backend.dto.RejectProofRequest;
import com.daswandh.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Everything under /api/admin/** requires the ADMIN role (see SecurityConfig).
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReviewService reviewService;

    public AdminController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/proofs")
    public List<ProofResponse> pendingProofs() {
        return reviewService.pendingProofs();
    }

    @PostMapping("/proofs/{proofId}/approve")
    public ProofResponse approve(@AuthenticationPrincipal UserDetails principal, @PathVariable Long proofId) {
        return reviewService.approve(principal.getUsername(), proofId);
    }

    @PostMapping("/proofs/{proofId}/reject")
    public ProofResponse reject(@AuthenticationPrincipal UserDetails principal, @PathVariable Long proofId,
                                @Valid @RequestBody RejectProofRequest request) {
        return reviewService.reject(principal.getUsername(), proofId, request.note());
    }

    @PostMapping("/milestones/{milestoneId}/release")
    public MilestoneResponse release(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long milestoneId) {
        return reviewService.release(principal.getUsername(), milestoneId);
    }
}