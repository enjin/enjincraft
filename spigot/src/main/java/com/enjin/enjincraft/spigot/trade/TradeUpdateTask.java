package com.enjin.enjincraft.spigot.trade;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.http.HttpResponse;
import com.enjin.sdk.model.service.requests.*;
import com.enjin.sdk.model.service.tokens.TokenEvent;
import com.enjin.sdk.model.service.tokens.TokenEventType;
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

            List<Transaction> data = getMostRecentTransaction(session);

            if (data.isEmpty())
                return;

            processTransaction(session, data.get(0));
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    private List<Transaction> getMostRecentTransaction(TradeSession session) throws IOException {
        HttpResponse<GraphQLResponse<List<Transaction>>> networkResponse = bootstrap.getTrustedPlatformClient()
                .getRequestService().getRequestsSync(new GetRequests()
                        .requestId(session.getMostRecentRequestId()));

        if (!networkResponse.isSuccess())
            throw new NetworkException(networkResponse.code());

        GraphQLResponse<List<Transaction>> graphQLResponse = networkResponse.body();

        if (!graphQLResponse.isSuccess())
            throw new GraphQLException(graphQLResponse.getErrors());

        return graphQLResponse.getData();
    }

    private TokenEvent getTokenEvent(Transaction transaction) {
        return transaction.getEvents().stream()
                .filter(e -> e.getEvent() == TokenEventType.CREATE_TRADE
                        || e.getEvent() == TokenEventType.COMPLETE_TRADE)
                .findFirst().orElse(null);
    }

    private void processTransaction(TradeSession session, Transaction transaction) {
        TransactionState state = transaction.getState();
        TokenEvent event =  getTokenEvent(transaction);

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
    }
}
