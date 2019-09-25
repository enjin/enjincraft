package com.enjin.ecmp.spigot.trade;

import com.enjin.ecmp.spigot.enums.TradeState;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ToString
public class TradeSession {

    private static final long LIFETIME = TimeUnit.DAYS.toSeconds(5);

    private int id;
    private int createRequestId;
    private int completeRequestId;
    private String tradeId;
    private UUID inviterUuid;
    private int inviterIdentityId;
    private String inviterEthAddr;
    private UUID invitedUuid;
    private int invitedIdentityId;
    private String invitedEthAddr;
    private OffsetDateTime createdAt;
    private TradeState state;

    public TradeSession(ResultSet rs) throws SQLException {
        id = rs.getInt("id");
        createRequestId = rs.getInt("createRequestId");
        completeRequestId = rs.getInt("completeRequestId");
        tradeId = rs.getString("tradeId");
        inviterUuid = UUID.fromString(rs.getString("inviterUuid"));
        inviterIdentityId = rs.getInt("inviterIdentityId");
        inviterEthAddr = rs.getString("inviterEthAddr");
        invitedUuid = UUID.fromString(rs.getString("invitedUuid"));
        invitedIdentityId = rs.getInt("invitedIdentityId");
        invitedEthAddr = rs.getString("invitedEthAddr");
        state = TradeState.valueOf(rs.getString("state"));
        createdAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(rs.getLong("createdAt")), ZoneOffset.UTC);
    }

    public boolean isExpired() {
        long diff = ChronoUnit.SECONDS.between(createdAt, OffsetDateTime.now(ZoneOffset.UTC));
        return diff >= LIFETIME;
    }

    public int getMostRecentRequestId() {
        return completeRequestId > 0 ? completeRequestId : createRequestId;
    }

    public int getId() {
        return id;
    }

    public int getCreateRequestId() {
        return createRequestId;
    }

    public int getCompleteRequestId() {
        return completeRequestId;
    }

    public String getTradeId() {
        return tradeId;
    }

    public UUID getInviterUuid() {
        return inviterUuid;
    }

    public int getInviterIdentityId() {
        return inviterIdentityId;
    }

    public String getInviterEthAddr() {
        return inviterEthAddr;
    }

    public UUID getInvitedUuid() {
        return invitedUuid;
    }

    public int getInvitedIdentityId() {
        return invitedIdentityId;
    }

    public String getInvitedEthAddr() {
        return invitedEthAddr;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public TradeState getState() {
        return state;
    }
}
