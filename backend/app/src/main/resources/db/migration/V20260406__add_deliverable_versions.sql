CREATE TABLE IF NOT EXISTS deliverable_version (
    id BIGSERIAL PRIMARY KEY,
    deliverable_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(64) NOT NULL,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_deliverable_version_deliverable FOREIGN KEY (deliverable_id) REFERENCES deliverable(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS deliverable_version_attachment (
    id BIGSERIAL PRIMARY KEY,
    deliverable_version_id BIGINT NOT NULL,
    file_url TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_size BIGINT NULL,
    file_type TEXT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_deliverable_version_attachment_version FOREIGN KEY (deliverable_version_id) REFERENCES deliverable_version(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_deliverable_version_deliverable_version
    ON deliverable_version (deliverable_id, version_number DESC);

CREATE INDEX IF NOT EXISTS idx_deliverable_version_attachment_version
    ON deliverable_version_attachment (deliverable_version_id);
