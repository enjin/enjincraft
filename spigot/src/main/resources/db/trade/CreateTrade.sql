INSERT INTO TradeSessions("inviterUuid",
                          "inviterIdentityId",
                          "inviterEthAddr",
                          "invitedUuid",
                          "invitedIdentityId",
                          "invitedEthAddr",
                          "createRequestId",
                          "state",
                          "createdAt")
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);