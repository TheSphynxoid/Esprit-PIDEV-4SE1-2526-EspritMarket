-- Ensure schema compatibility for Delivery entity field: cancellationReason -> delivery.cancellation_reason
ALTER TABLE IF EXISTS delivery
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(255);
