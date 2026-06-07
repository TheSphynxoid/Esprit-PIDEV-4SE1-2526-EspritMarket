-- Delivery branch: Add package dimensions, weight, and base order amount to delivery
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS real_weight NUMERIC(10,3);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS length NUMERIC(10,3);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS width NUMERIC(10,3);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS height NUMERIC(10,3);
ALTER TABLE delivery ADD COLUMN IF NOT EXISTS base_order_amount NUMERIC(10,3);

-- Delivery branch: Add payment method to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(255);

-- Delivery branch: Add interview reminder tracking to quiz_result
ALTER TABLE quiz_result ADD COLUMN IF NOT EXISTS interview_reminder_sent_at TIMESTAMP;
