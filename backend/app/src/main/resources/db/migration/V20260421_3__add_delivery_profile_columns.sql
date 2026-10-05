-- Backfill Delivery schema columns introduced after legacy deployments.
ALTER TABLE IF EXISTS delivery
    ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city VARCHAR(255),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS connected_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS delivery_mode VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(255),
    ADD COLUMN IF NOT EXISTS distance_km NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS qr_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_details_id BIGINT;
