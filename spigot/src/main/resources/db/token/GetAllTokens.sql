SELECT T."token_id"         AS "token_id",
    I."token_index"         AS "token_index",
    T."nonfungible"         AS "nonfungible",
    T."alternate_id"        AS "alternate_id",
    I."nbt"                 AS "nbt",
    T."metadata_uri"        AS "metadata_uri",
    T."wallet_view_state"   AS "wallet_view_state"
FROM token AS T
    LEFT JOIN token_instance AS I
        ON (I."token_id" = T."token_id");
