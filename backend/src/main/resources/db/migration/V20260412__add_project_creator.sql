ALTER TABLE project ADD COLUMN IF NOT EXISTS creator_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_project_creator'
        AND table_name = 'project'
    ) THEN
        ALTER TABLE project
            ADD CONSTRAINT fk_project_creator
            FOREIGN KEY (creator_id) REFERENCES app_user(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_project_creator_id ON project (creator_id);
