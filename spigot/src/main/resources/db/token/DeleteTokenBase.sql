begin;
DELETE FROM token
    WHERE "token_id" = ?;
DELETE FROM token_instance
    WHERE "token_id" = ?;
commit;
