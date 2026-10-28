package com.daswandh.backend.dto;

// raised = all money in, released = all money handed to the creator, available = what's still held.
public record LedgerSummary(long raisedPaise, long releasedPaise, long availablePaise) {

    public static LedgerSummary of(long raisedPaise, long releasedPaise) {
        return new LedgerSummary(raisedPaise, releasedPaise, raisedPaise - releasedPaise);
    }
}