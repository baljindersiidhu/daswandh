-- ============ USERS ============
CREATE TABLE users (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,           -- BCrypt hashes are 60 chars
                       full_name     VARCHAR(100) NOT NULL,
                       role          VARCHAR(20)  NOT NULL CHECK (role IN ('DONOR', 'CREATOR', 'ADMIN')),
                       enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT uq_users_email UNIQUE (email)
);

-- ============ CAMPAIGNS ============
-- All money columns are BIGINT paise (1 rupee = 100 paise). Never floating point.
CREATE TABLE campaigns (
                           id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           creator_id  BIGINT        NOT NULL REFERENCES users (id),
                           title       VARCHAR(150)  NOT NULL,
                           description VARCHAR(5000) NOT NULL,
                           goal_paise  BIGINT        NOT NULL CHECK (goal_paise > 0),
                           deadline    DATE          NOT NULL,
                           status      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                               CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
                           created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_campaigns_creator ON campaigns (creator_id);
CREATE INDEX idx_campaigns_status  ON campaigns (status);

-- ============ MILESTONES ============
-- LOCKED -> RELEASED -> PROOF_SUBMITTED -> VERIFIED
CREATE TABLE milestones (
                            id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            campaign_id  BIGINT        NOT NULL REFERENCES campaigns (id),
                            sequence_no  INT           NOT NULL CHECK (sequence_no > 0),
                            title        VARCHAR(150)  NOT NULL,
                            description  VARCHAR(2000),
                            amount_paise BIGINT        NOT NULL CHECK (amount_paise > 0),
                            status       VARCHAR(20)   NOT NULL DEFAULT 'LOCKED'
                                CHECK (status IN ('LOCKED', 'RELEASED', 'PROOF_SUBMITTED', 'VERIFIED')),
                            CONSTRAINT uq_milestone_sequence UNIQUE (campaign_id, sequence_no)
);

-- ============ DONATIONS ============
CREATE TABLE donations (
                           id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           campaign_id         BIGINT      NOT NULL REFERENCES campaigns (id),
                           donor_id            BIGINT      NOT NULL REFERENCES users (id),
                           amount_paise        BIGINT      NOT NULL CHECK (amount_paise > 0),
                           status              VARCHAR(20) NOT NULL DEFAULT 'CREATED'
                               CHECK (status IN ('CREATED', 'PAID', 'FAILED')),
                           is_anonymous        BOOLEAN     NOT NULL DEFAULT FALSE,
                           razorpay_order_id   VARCHAR(64) NOT NULL,
                           razorpay_payment_id VARCHAR(64),                -- NULL until the payment happens
                           created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                           CONSTRAINT uq_donations_order   UNIQUE (razorpay_order_id),
                           CONSTRAINT uq_donations_payment UNIQUE (razorpay_payment_id)  -- NULLs are allowed many times
);
CREATE INDEX idx_donations_campaign ON donations (campaign_id);

-- ============ LEDGER (append-only) ============
CREATE TABLE ledger_entries (
                                id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                campaign_id  BIGINT       NOT NULL REFERENCES campaigns (id),
                                entry_type   VARCHAR(30)  NOT NULL
                                    CHECK (entry_type IN ('DONATION_IN', 'MILESTONE_RELEASE_OUT')),
                                amount_paise BIGINT       NOT NULL CHECK (amount_paise > 0),
                                donation_id  BIGINT       REFERENCES donations (id),
                                milestone_id BIGINT       REFERENCES milestones (id),
                                description  VARCHAR(255) NOT NULL,
                                created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- A credit must point at a donation, a release must point at a milestone, never both.
                                CONSTRAINT chk_ledger_reference CHECK (
                                    (entry_type = 'DONATION_IN'           AND donation_id  IS NOT NULL AND milestone_id IS NULL) OR
                                    (entry_type = 'MILESTONE_RELEASE_OUT' AND milestone_id IS NOT NULL AND donation_id  IS NULL)
                                    )
);
CREATE INDEX idx_ledger_campaign ON ledger_entries (campaign_id);

-- Idempotency safety net: a donation can be credited once, a milestone released once.
-- Even if two webhook calls race each other, the second insert fails.
CREATE UNIQUE INDEX uq_ledger_one_credit_per_donation
    ON ledger_entries (donation_id) WHERE entry_type = 'DONATION_IN';
CREATE UNIQUE INDEX uq_ledger_one_release_per_milestone
    ON ledger_entries (milestone_id) WHERE entry_type = 'MILESTONE_RELEASE_OUT';

-- Append-only enforcement: any UPDATE, DELETE or TRUNCATE is rejected by the database.
CREATE FUNCTION prevent_ledger_modification() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_no_update_delete
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION prevent_ledger_modification();

CREATE TRIGGER trg_ledger_no_truncate
    BEFORE TRUNCATE ON ledger_entries
    FOR EACH STATEMENT EXECUTE FUNCTION prevent_ledger_modification();

-- ============ PROOFS ============
CREATE TABLE proofs (
                        id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        milestone_id       BIGINT        NOT NULL REFERENCES milestones (id),
                        uploaded_by        BIGINT        NOT NULL REFERENCES users (id),
                        file_url           VARCHAR(500)  NOT NULL,
                        image_hash         VARCHAR(64)   NOT NULL,       -- SHA-256 hex of the uploaded file
                        description        VARCHAR(1000),
                        amount_spent_paise BIGINT        NOT NULL CHECK (amount_spent_paise > 0),
                        status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
                        reviewed_by        BIGINT        REFERENCES users (id),
                        review_note        VARCHAR(1000),
                        reviewed_at        TIMESTAMPTZ,
                        created_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_proofs_milestone ON proofs (milestone_id);
-- Not unique on purpose: a duplicate image should raise a fraud flag, not crash the upload.
CREATE INDEX idx_proofs_image_hash ON proofs (image_hash);

-- ============ FRAUD FLAGS ============
CREATE TABLE fraud_flags (
                             id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             campaign_id BIGINT        NOT NULL REFERENCES campaigns (id),
                             proof_id    BIGINT        REFERENCES proofs (id),
                             flag_type   VARCHAR(40)   NOT NULL
                                 CHECK (flag_type IN ('DUPLICATE_PROOF_IMAGE', 'NO_PROOF_TIMEOUT')),
                             details     VARCHAR(1000) NOT NULL,
                             status      VARCHAR(20)   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
                             created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_fraud_flags_campaign ON fraud_flags (campaign_id);

