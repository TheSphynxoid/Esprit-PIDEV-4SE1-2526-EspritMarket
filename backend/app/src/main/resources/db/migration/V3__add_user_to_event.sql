-- Add user_id column to event table
ALTER TABLE event ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Add foreign key constraint
ALTER TABLE event ADD CONSTRAINT fk_event_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

-- Create index
CREATE INDEX IF NOT EXISTS idx_event_user_id ON event(user_id);
