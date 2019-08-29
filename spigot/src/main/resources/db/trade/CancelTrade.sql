UPDATE TradeSessions
SET "state" = ?
WHERE "createRequestId" = ?
OR "completeRequestId" = ?;