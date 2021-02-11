SELECT "permission", "world"
FROM token_permission
WHERE "token_id" = ? AND "token_index" = ?;