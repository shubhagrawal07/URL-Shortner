CREATE TABLE short_links (
    id BIGSERIAL PRIMARY KEY,
    long_url TEXT NOT NULL,
    short_code VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_short_links_long_url UNIQUE (long_url),
    CONSTRAINT uq_short_links_short_code UNIQUE (short_code)
);
