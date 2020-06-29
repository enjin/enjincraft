CREATE TABLE IF NOT EXISTS token
(
    "token_id"    VARCHAR(16) NOT NULL,
    "token_index" VARCHAR(16) NOT NULL,
    "nbt"         TEXT NOT NULL,
    PRIMARY KEY ("token_id", "token_index")
)
