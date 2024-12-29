CREATE TABLE IF NOT EXISTS urls (
    id          BIGSERIAL PRIMARY KEY,
    short_code  VARCHAR(20)  NOT NULL UNIQUE,
    original_url TEXT        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    click_count BIGINT       NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  VARCHAR(100),
    description VARCHAR(500)
);

CREATE INDEX idx_urls_short_code  ON urls (short_code);
CREATE INDEX idx_urls_expires_at  ON urls (expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_urls_active      ON urls (active);

CREATE TABLE IF NOT EXISTS url_clicks (
    id          BIGSERIAL PRIMARY KEY,
    url_id      BIGINT       NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
    clicked_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(500),
    referer     VARCHAR(1000)
);

CREATE INDEX idx_url_clicks_url_id    ON url_clicks (url_id);
CREATE INDEX idx_url_clicks_clicked_at ON url_clicks (clicked_at);
