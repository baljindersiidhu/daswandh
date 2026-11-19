package com.daswandh.backend.controller;

import com.daswandh.backend.dto.ProofResponse;
import com.daswandh.backend.service.ProofService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ProofController {

    private final ProofService proofService;

    public ProofController(ProofService proofService) {
        this.proofService = proofService;
    }

    // multipart/form-data: a file plus normal form fields in one request
    @PostMapping(path = "/api/milestones/{milestoneId}/proofs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProofResponse submit(@AuthenticationPrincipal UserDetails principal,
                                @PathVariable Long milestoneId,
                                @RequestParam("file") MultipartFile file,
                                @RequestParam("description") String description,
                                @RequestParam("amountSpentPaise") long amountSpentPaise) {
        return proofService.submitProof(principal.getUsername(), milestoneId, file, description, amountSpentPaise);
    }

    @GetMapping("/api/milestones/{milestoneId}/proofs")
    public List<ProofResponse> list(@PathVariable Long milestoneId) {
        return proofService.listForMilestone(milestoneId);
    }
}