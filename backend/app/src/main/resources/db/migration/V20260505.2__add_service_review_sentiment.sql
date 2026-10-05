ALTER TABLE service_review 
ADD COLUMN IF NOT EXISTS sentiment VARCHAR(50),
ADD COLUMN IF NOT EXISTS sentiment_confidence NUMERIC(5, 4);
