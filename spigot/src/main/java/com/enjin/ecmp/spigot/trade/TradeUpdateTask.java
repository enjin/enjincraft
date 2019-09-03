package com.enjin.ecmp.spigot.trade;

import com.enjin.ecmp.spigot.GraphQLException;
import com.enjin.ecmp.spigot.NetworkException;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.requests.*;
import com.enjin.enjincoin.sdk.model.service.requests.data.CompleteTradeData;
import com.enjin.enjincoin.sdk.model.service.tokens.TokenEvent;
import com.enjin.enjincoin.sdk.model.service.tokens.TokenEventType;
import com.enjin.java_commons.StringUtils;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TradeUpdateTask extends BukkitRunnable {

    private SpigotBootstrap bootstrap;
    private List<TradeSession> tradeSessions;

    public TradeUpdateTask(SpigotBootstrap bootstrap) throws SQLException {
        this.bootstrap = bootstrap;
        this.tradeSessions = bootstrap.db().getPendingTrades();
    }

    @Override
    public void run() {
        try {
            if (tradeSessions.isEmpty()) {
                if (!isCancelled())
                    cancel();
                return;
            }

            TradeSession session = tradeSessions.remove(0);

            if (session.isExpired()) {
                bootstrap.getTradeManager().cancelTrade(session.getMostRecentRequestId());
                return;
            }

            HttpResponse<GraphQLResponse<List<Transaction>>> networkResponse = bootstrap.getTrustedPlatformClient()
                    .getRequestsService().getRequestsSync(new GetRequests()
                            .requestId(session.getMostRecentRequestId()));

            if (!networkResponse.isSuccess())
                throw new NetworkException(networkResponse.code());

            GraphQLResponse<List<Transaction>> graphQLResponse = networkResponse.body();

            if (!graphQLResponse.isSuccess())
                throw new GraphQLException(graphQLResponse.getErrors());

            List<Transaction> data = graphQLResponse.getData();

            if (data.isEmpty())
                return;

            Transaction transaction = data.get(0);
            TransactionState state = transaction.getState();
            TokenEvent event = transaction.getEvents().stream()
                    .filter(e -> e.getEvent() == TokenEventType.CREATE_TRADE
                            || e.getEvent() == TokenEventType.COMPLETE_TRADE)
                    .findFirst().orElse(null);

            if (event == null)
                return;

            TokenEventType type = event.getEvent();

            if (type == TokenEventType.CREATE_TRADE) {
                if (state == TransactionState.CANCELED_USER || state == TransactionState.CANCELED_PLATFORM) {
                    bootstrap.getTradeManager().cancelTrade(transaction.getId());
                } else if (state == TransactionState.EXECUTED) {
                    bootstrap.getTradeManager().sendCompleteRequest(session, event.getParam1());
                }
            } else if (type == TokenEventType.COMPLETE_TRADE) {
                if (state == TransactionState.CANCELED_USER || state == TransactionState.CANCELED_PLATFORM) {
                    bootstrap.getTradeManager().cancelTrade(transaction.getId());
                } else if (state == TransactionState.EXECUTED) {
                    bootstrap.getTradeManager().completeTrade(session);
                }
            }
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

}
