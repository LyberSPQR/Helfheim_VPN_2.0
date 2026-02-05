CREATE TABLE users(
    user_id BIGSERIAL PRIMARY KEY ,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    public_key TEXT UNIQUE ,
    private_key TEXT UNIQUE ,
    is_active BOOLEAN DEFAULT FALSE,
    subscription_expires_at BIGINT
);
CREATE TABLE ip_pool(
    id BIGSERIAL PRIMARY KEY ,
    user_id BIGINT REFERENCES users(user_id) UNIQUE,
    ip_address inet UNIQUE NOT NULL,
    is_assigned BOOLEAN DEFAULT FALSE
);

CREATE INDEX ip_pool_index ON ip_pool(is_assigned)
WHERE is_assigned = FALSE;

INSERT INTO ip_pool (ip_address, is_assigned)
SELECT
    ('10.10.0.0'::inet + i) AS ip_address,
    FALSE AS is_assigned
FROM generate_series(2, 65534) AS i;