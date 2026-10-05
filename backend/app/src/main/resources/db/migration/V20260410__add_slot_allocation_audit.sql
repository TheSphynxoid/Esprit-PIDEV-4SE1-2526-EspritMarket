CREATE TABLE IF NOT EXISTS slot_allocation_audit (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL,
    project_id BIGINT,
    mode VARCHAR(40) NOT NULL,
    slot_start TIMESTAMP,
    slot_end TIMESTAMP,
    final_score DOUBLE PRECISION NOT NULL,
    reason_code VARCHAR(80),
    policy_profile VARCHAR(40),
    tie_breaker_weight DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_slot_allocation_audit_project_created
    ON slot_allocation_audit (project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_slot_allocation_audit_service_created
    ON slot_allocation_audit (service_id, created_at DESC);
