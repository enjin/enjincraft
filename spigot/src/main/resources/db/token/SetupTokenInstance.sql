CREATE TABLE IF NOT EXISTS token_instance
(
    "token_id"      VARCHAR(16) NOT NULL,
    "token_index"   VARCHAR(16) NOT NULL,
    "nbt"           TEXT NULLABLE,
    "metadata_uri"  TEXT NULLABLE,

    PRIMARY KEY ("token_id", "token_index"),
    FOREIGN KEY ("token_id")
        REFERENCES token ("token_id")
        ON DELETE CASCADE
)
