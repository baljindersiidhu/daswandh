package com.daswandh.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WebhookEventStore {

    private final JdbcTemplate jdbcTemplate;

    public WebhookEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Atomically "claims" an event id. Returns true the first time, false for a duplicate.
    // If two identical deliveries race, the second INSERT waits for the first transaction to finish
    // and then does nothing, so exactly one of them gets true.
    public boolean claim(String eventId, String eventType) {
        int inserted = jdbcTemplate.update(
                "INSERT INTO webhook_events (event_id, event_type) VALUES (?, ?) ON CONFLICT (event_id) DO NOTHING",
                eventId, eventType);
        return inserted == 1;
    }
}