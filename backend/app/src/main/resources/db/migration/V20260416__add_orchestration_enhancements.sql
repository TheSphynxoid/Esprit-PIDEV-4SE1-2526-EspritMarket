CREATE TABLE IF NOT EXISTS milestone_bookings (
    milestone_id BIGINT NOT NULL REFERENCES project_milestone(id) ON DELETE CASCADE,
    booking_id   BIGINT NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    PRIMARY KEY (milestone_id, booking_id)
);

CREATE INDEX IF NOT EXISTS idx_milestone_bookings_booking_id
    ON milestone_bookings(booking_id);

CREATE INDEX IF NOT EXISTS idx_milestone_bookings_milestone_id
    ON milestone_bookings(milestone_id);
