ALTER TABLE booking ADD COLUMN IF NOT EXISTS project_id BIGINT;

ALTER TABLE booking
    ADD CONSTRAINT fk_booking_project
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_booking_project_id ON booking (project_id);
