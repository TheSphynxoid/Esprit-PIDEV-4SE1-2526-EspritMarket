CREATE TABLE IF NOT EXISTS ml_training_data (
    id BIGSERIAL PRIMARY KEY,
    model_type VARCHAR(30) NOT NULL,
    features JSONB NOT NULL,
    label INTEGER NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'APPLICATION',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ml_training_data_model ON ml_training_data(model_type);
CREATE INDEX idx_ml_training_data_source ON ml_training_data(source);
