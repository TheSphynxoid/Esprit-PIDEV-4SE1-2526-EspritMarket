-- Add estimated_hours to milestone_services junction table
ALTER TABLE milestone_services ADD COLUMN IF NOT EXISTS estimated_hours DOUBLE PRECISION DEFAULT 2.0;

-- Add favor_projects flag to service providers
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS favor_projects BOOLEAN DEFAULT FALSE;
