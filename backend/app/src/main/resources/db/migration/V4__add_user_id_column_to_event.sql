-- Add user_id column to event table if it doesn't already exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'event' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE event ADD COLUMN user_id BIGINT;
        
        -- Set default value for existing rows
        UPDATE event SET user_id = 1 WHERE user_id IS NULL;
        
        -- Make column NOT NULL
        ALTER TABLE event ALTER COLUMN user_id SET NOT NULL;
        
        -- Add foreign key constraint
        ALTER TABLE event ADD CONSTRAINT fk_event_user 
            FOREIGN KEY (user_id) REFERENCES app_user(id) 
            ON DELETE CASCADE;
        
        -- Create index for performance
        CREATE INDEX idx_event_user_id ON event(user_id);
    END IF;
END $$;
