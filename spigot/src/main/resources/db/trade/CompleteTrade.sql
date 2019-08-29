UPDATE TradeSessions
SET "completeRequestId" = ?,
    "tradeId"           = ?,
    "state"             = ?
WHERE "createRequestId" = ?;