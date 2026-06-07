-- Create table expected by DeliveryAddressDetails entity for legacy schemas.
CREATE TABLE IF NOT EXISTS delivery_address_details (
    id BIGSERIAL PRIMARY KEY,
    delivery_address VARCHAR(255),
    city VARCHAR(255),
    postal_code VARCHAR(255),
    phone_number VARCHAR(255),
    connected_user_id BIGINT
);
