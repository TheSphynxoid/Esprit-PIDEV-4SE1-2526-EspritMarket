-- Marketplace ownership and promotion constraints.
-- This script is PostgreSQL-compatible and can be executed manually if Flyway/Liquibase is not enabled.

ALTER TABLE IF EXISTS store
    ADD CONSTRAINT IF NOT EXISTS uk_store_owner UNIQUE (owner_id);

ALTER TABLE IF EXISTS product
    ALTER COLUMN store_id SET NOT NULL;

ALTER TABLE IF EXISTS product
    ADD COLUMN IF NOT EXISTS original_price DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS discount_percent DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS promo_start_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS promo_end_at TIMESTAMP;

ALTER TABLE IF EXISTS product
    ADD CONSTRAINT IF NOT EXISTS chk_product_discount_percent
    CHECK (discount_percent IS NULL OR (discount_percent > 0 AND discount_percent <= 100));

ALTER TABLE IF EXISTS product
    ADD CONSTRAINT IF NOT EXISTS chk_product_promo_dates
    CHECK (
        (promo_start_at IS NULL AND promo_end_at IS NULL)
        OR (promo_start_at IS NOT NULL AND promo_end_at IS NOT NULL AND promo_end_at > promo_start_at)
    );
