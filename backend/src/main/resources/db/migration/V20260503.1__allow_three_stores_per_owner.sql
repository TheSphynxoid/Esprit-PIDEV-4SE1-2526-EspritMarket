-- Allow up to 3 stores per seller by removing one-store-per-owner uniqueness.
-- Keep a non-unique index for owner-based lookups.

ALTER TABLE IF EXISTS store
    DROP CONSTRAINT IF EXISTS uk_store_owner;

CREATE INDEX IF NOT EXISTS idx_store_owner_id ON store(owner_id);
