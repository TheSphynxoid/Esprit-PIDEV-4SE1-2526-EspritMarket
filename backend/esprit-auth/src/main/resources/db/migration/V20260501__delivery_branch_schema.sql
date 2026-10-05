-- Delivery branch schema additions.
-- Already applied via ddl-auto=update on shared DB; this migration ensures consistency.

CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    "read" BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE courier ADD COLUMN IF NOT EXISTS profile_status VARCHAR(50) DEFAULT 'INCOMPLETE';
