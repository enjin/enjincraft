CREATE TABLE IF NOT EXISTS TradeSessions
(
    "id"                INTEGER PRIMARY KEY AUTOINCREMENT,
    "createRequestId"   INTEGER,
    "completeRequestId" INTEGER,
    "tradeId"           TEXT,
    "inviterUuid"       TEXT,
    "inviterIdentityId" TEXT,
    "inviterEthAddr"    INTEGER,
    "invitedUuid"       TEXT,
    "invitedIdentityId" TEXT,
    "invitedEthAddr"    INTEGER,
    "state"             TEXT,
    "createdAt"         INTEGER
)
