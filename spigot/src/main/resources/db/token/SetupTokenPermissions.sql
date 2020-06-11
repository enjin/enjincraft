CREATE TABLE IF NOT EXISTS token_permission
(
    "token_id"      VARCHAR(16),
    "token_index"   VARCHAR(16),
    "permission"    TEXT NOT NULL,
    "world"         TEXT NOT NULL,
    PRIMARY KEY ("token_id", "token_index", "permission", "world"),
    FOREIGN KEY ("token_id", "token_index") REFERENCES token("token_id", "token_index")
)
