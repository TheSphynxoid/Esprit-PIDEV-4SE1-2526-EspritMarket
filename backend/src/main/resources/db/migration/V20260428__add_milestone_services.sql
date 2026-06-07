CREATE TABLE IF NOT EXISTS milestone_services (
    milestone_id BIGINT NOT NULL,
    service_id   BIGINT NOT NULL,
    PRIMARY KEY (milestone_id, service_id),
    CONSTRAINT fk_ms_milestone FOREIGN KEY (milestone_id) REFERENCES project_milestone(id) ON DELETE CASCADE,
    CONSTRAINT fk_ms_service   FOREIGN KEY (service_id)   REFERENCES service(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ms_service_id ON milestone_services (service_id);
