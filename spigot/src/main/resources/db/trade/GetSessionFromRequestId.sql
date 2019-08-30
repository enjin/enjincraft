SELECT * FROM TradeSessions
WHERE createRequestId = ?
OR completeRequestId = ?;