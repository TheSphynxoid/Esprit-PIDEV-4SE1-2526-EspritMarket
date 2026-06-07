CREATE INDEX IF NOT EXISTS idx_booking_message_booking_id_id_desc
    ON booking_message (booking_id, id DESC);
