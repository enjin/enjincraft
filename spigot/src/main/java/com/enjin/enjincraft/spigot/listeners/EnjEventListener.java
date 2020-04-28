package com.enjin.enjincraft.spigot.listeners;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.sdk.models.notification.*;
import com.enjin.sdk.models.request.TransactionType;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EnjEventListener implements com.enjin.sdk.services.notification.NotificationListener {

    private SpigotBootstrap bootstrap;

    public EnjEventListener(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void notificationReceived(NotificationEvent event) {
        try {
            bootstrap.debug(String.format("Received event: %s", event));
            EventType eventType = event.getType();

            if (eventType == null)
                return;

            switch (eventType) {
                case TRADE_COMPLETED:
                case TRADE_CREATED:
                    onTradeExecuted(event);
                    break;
                case TRANSACTION_CANCELED:
                    onTxCancelled(event);
                    break;
                case IDENTITY_LINKED:
                    onIdentityLinked(event);
                    break;
                case IDENTITY_DELETED:
                case IDENTITY_UNLINKED:
                    onIdentityUnlinked(event);
                    break;
                case TOKEN_MELTED:
                case TOKEN_MINTED:
                case TOKEN_TRANSFERRED:
                    onTokenUpdated(event);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    private void onTradeExecuted(NotificationEvent event) {
        if (!event.getChannel().contains("app"))
            return;

        TransactionType type = getTransactionType(event);

        if (type == null)
            return;

        int transactionId;
        String tradeId;

        try {
            JsonObject transaction = event.getEventData()
                    .get("transaction")
                    .getAsJsonObject();
            JsonObject trade = event.getEventData()
                    .get("trade")
                    .getAsJsonObject();
            transactionId = transaction.get("id").getAsInt();
            tradeId = trade.get("id").getAsString();
        } catch (Exception e) {
            bootstrap.log(e);
            return;
        }

        switch (type) {
            case CREATE_TRADE:
                bootstrap.getTradeManager().sendCompleteRequest(transactionId, tradeId);
                break;
            case SEND:
                bootstrap.getTradeManager().completeTrade(transactionId);
                break;
            default:
                break;
        }
    }

    private void onTxCancelled(NotificationEvent event) {
        if (!event.getChannel().contains("app"))
            return;

        TransactionType type = getTransactionType(event);

        if (type == null)
            return;

        int transactionId;

        try {
            JsonObject transaction = event.getEventData()
                    .get("transaction")
                    .getAsJsonObject();
            transactionId = transaction.get("id").getAsInt();
        } catch (Exception e) {
            bootstrap.log(e);
            return;
        }

        switch (type) {
            case CREATE_TRADE:
            case COMPLETE_TRADE:
                bootstrap.getTradeManager().cancelTrade(transactionId);
                break;
            default:
                break;
        }
    }

    private void onIdentityLinked(NotificationEvent event) {
        if (!event.getChannel().contains("identity"))
            return;

        JsonObject identity = event.getEventData().getAsJsonObject("identity");

        if (identity == null || identity.get("id") == null)
            return;

        int id = identity.get("id").getAsInt();

        EnjPlayer enjPlayer = bootstrap.getPlayerManager().getPlayer(id).orElse(null);
        if (enjPlayer == null)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), enjPlayer::reloadIdentity);
    }

    private void onIdentityUnlinked(NotificationEvent event) {
        if (!event.getChannel().contains("identity"))
            return;

        JsonObject identity = event.getEventData().getAsJsonObject("identity");

        if (identity == null || identity.get("id") == null)
            return;

        int id = identity.get("id").getAsInt();

        Optional<EnjPlayer> playerOptional = bootstrap.getPlayerManager().getPlayer(id);
        playerOptional.ifPresent(player -> {
            Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), player::unlinked);
        });
    }

    private void onTokenUpdated(NotificationEvent event) {
        if (!event.getChannel().contains("app"))
            return;

        TransactionType type = getTransactionType(event);

        if (type == null)
            return;

        switch (type) {
            case MELT:
            case MINT:
            case SEND:
                updateBalance(type, event.getEventData());
                break;
            default:
                break;
        }
    }

    private void updateBalance(TransactionType type, JsonObject data) {
        String tokenId;
        int amount;

        try {
            tokenId = data.get("token")
                    .getAsJsonObject()
                    .get("id")
                    .getAsString();
        } catch (Exception e) {
            bootstrap.log(e);
            return;
        }

        if (type == TransactionType.SEND) {
            String from;
            String to;

            try {
                JsonObject transfer = data.get("transfer").getAsJsonObject();
                from = transfer.get("from").getAsString();
                to = transfer.get("to").getAsString();
                amount = transfer.get("value").getAsInt();
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            updateBalance(from, tokenId, -amount);
            updateBalance(to, tokenId, amount);
        } else if (type == TransactionType.MELT) {
            String ethAddr;

            try {
                JsonObject melt = data.get("melt").getAsJsonObject();
                ethAddr = melt.get("owner").getAsString();
                amount = melt.get("value").getAsInt();
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            updateBalance(ethAddr, tokenId, -amount);
        } else if (type == TransactionType.MINT) {
            String ethAddr;

            try {
                JsonObject mint = data.get("mint").getAsJsonObject();
                JsonObject transaction = data.get("transaction").getAsJsonObject();
                ethAddr = mint.get("to").getAsString();
                amount = transaction.get("value").getAsInt();
            } catch (Exception e) {
                bootstrap.log(e);
                return;
            }

            updateBalance(ethAddr, tokenId, amount);
        }
    }

    private void updateBalance(String ethAddr, String tokenId, int balanceDelta) {
        EnjPlayer enjPlayer = bootstrap.getPlayerManager().getPlayer(ethAddr).orElse(null);

        if (enjPlayer == null || enjPlayer.getTokenWallet() == null)
            return;

        TokenModel tokenModel = bootstrap.getTokenManager().getToken(tokenId);
        MutableBalance mBalance = enjPlayer.getTokenWallet().getBalance(tokenId);

        if (mBalance == null && balanceDelta > 0) {
            mBalance = new MutableBalance(tokenId, null, balanceDelta);
            enjPlayer.getTokenWallet().setBalance(mBalance);

            // Adds the token's permissions to the player
            for (Map.Entry<String, Set<String>> entry : tokenModel.getPermissionsMap().entrySet()) {
                String world = entry.getKey();
                Set<String> perms = entry.getValue();

                perms.forEach(perm -> { enjPlayer.addPermission(perm, tokenId, world); });
            }
        } else if (mBalance != null) {
            int balance = mBalance.balance() + balanceDelta;

            if (balance > 0) {
                mBalance.set(balance);
            } else {
                enjPlayer.getTokenWallet().removeBalance(tokenId);

                // Removes the token's permissions from the player
                for (Map.Entry<String, Set<String>> entry : tokenModel.getPermissionsMap().entrySet()) {
                    String world = entry.getKey();
                    Set<String> perms = entry.getValue();

                    perms.forEach(perm -> { enjPlayer.removePermission(perm, world); });
                }
            }
        }

        enjPlayer.validateInventory();
    }

    private TransactionType getTransactionType(NotificationEvent event) {
        String typeString;

        try {
            JsonObject transaction = event.getEventData()
                    .get("transaction")
                    .getAsJsonObject();
            typeString = transaction.get("type").getAsString();
        } catch (Exception e) {
            bootstrap.log(e);
            return null;
        }

        for (TransactionType transactionType : TransactionType.values()) {
            if (typeString.equalsIgnoreCase(transactionType.name()))
                return transactionType;
        }

        bootstrap.debug(String.format("No such transaction type: %s", typeString));

        return null;
    }

}
