-- Ensure schema compatibility for Courier entity field: createdAt -> courier.created_at
ALTER TABLE IF EXISTS courier
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE courier
SET created_at = NOW()
WHERE created_at IS NULL;
