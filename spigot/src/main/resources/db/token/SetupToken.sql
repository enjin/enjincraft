CREATE TABLE IF NOT EXISTS token
(
    "token_id"          VARCHAR(16) NOT NULL,
    "nonfungible"       BOOLEAN NOT NULL DEFAULT 0,
    "alternate_id"      TEXT NULLABLE,
    "metadata_uri"      TEXT NULLABLE,
    "wallet_view_state" TEXT NULLABLE,

    CHECK ("nonfungible" IN (0,1)),

    PRIMARY KEY ("token_id")
)
