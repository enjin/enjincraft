UPDATE token
SET "alternate_id" = ?,
    "metadata_uri" = ?,
    "wallet_view_state" = ?
WHERE "token_id" = ?;