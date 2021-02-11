DELETE FROM token_instance
    WHERE "token_id" = ?
        AND "token_index" = ?;