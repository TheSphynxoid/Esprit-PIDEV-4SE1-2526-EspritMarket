-- Add nb_tickets column to event table
ALTER TABLE event ADD COLUMN nb_tickets INT NOT NULL DEFAULT 0;

-- Add quantity column to equipment table
ALTER TABLE equipment ADD COLUMN quantity INT NOT NULL DEFAULT 1;
