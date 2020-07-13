UPDATE token
SET "alternate_id" = ?,
    "wallet_view_state" = ?
WHERE "token_id" = ?;