-- Increase logo_url column length to support larger URLs and file paths
ALTER TABLE store
    ALTER COLUMN logo_url TYPE TEXT;

-- Create index on logo_url if needed for performance
CREATE INDEX IF NOT EXISTS idx_store_logo_url ON store(logo_url);
