UPDATE token_instance
SET "nbt" = ?,
    "metadata_uri" = ?
WHERE "token_id" = ? AND "token_index" = ?;