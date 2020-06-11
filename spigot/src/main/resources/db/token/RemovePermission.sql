DELETE FROM token_permission
WHERE "token_id" = ?
    AND "token_index" = ?
    AND "permission" = ?
    AND "world" = ?;