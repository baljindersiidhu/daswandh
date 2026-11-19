-- One row per webhook event we have claimed. The UNIQUE constraint is what makes
-- "have I seen this event before?" a race-proof question.
CREATE TABLE webhook_events (
                                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                event_id    VARCHAR(100) NOT NULL,
                                event_type  VARCHAR(50)  NOT NULL,
                                received_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                CONSTRAINT uq_webhook_events_event_id UNIQUE (event_id)
);