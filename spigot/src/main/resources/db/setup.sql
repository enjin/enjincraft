CREATE TABLE IF NOT EXISTS "TradeSessions" (
	"id"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"createRequestId"	INTEGER,
	"completeRequestId"	INTEGER,
	"tradeId"	TEXT,
	"inviterUuid"	TEXT,
	"inviterEthAddr"	INTEGER,
	"invitedUuid"	TEXT,
	"invitedEthAddr"	INTEGER,
	"state"	TEXT
)