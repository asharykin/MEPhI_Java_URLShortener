CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS short_urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url VARCHAR(6) NOT NULL UNIQUE,
    long_url VARCHAR(2048) NOT NULL,
    creator_id UUID NOT NULL,
    use_count INTEGER NOT NULL,
    use_limit INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    ttl_hours INTEGER NOT NULL,
    deleted BOOLEAN NOT NULL,

    FOREIGN KEY (creator_id) REFERENCES users (id)
);