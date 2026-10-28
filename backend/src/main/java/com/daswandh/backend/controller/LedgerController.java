package com.daswandh.backend.controller;

import com.daswandh.backend.dto.LedgerResponse;
import com.daswandh.backend.service.LedgerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Read-only and public: this is the transparency data.
@RestController
@RequestMapping("/api/campaigns/{campaignId}/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    public LedgerResponse ledger(@PathVariable Long campaignId,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return ledgerService.getPublicLedger(campaignId, page, size);
    }
}