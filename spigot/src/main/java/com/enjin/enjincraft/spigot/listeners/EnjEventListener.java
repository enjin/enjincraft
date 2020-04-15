package com.enjin.enjincraft.spigot.listeners;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.sdk.models.notification.*;
import com.enjin.sdk.models.request.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnjEventListener implements com.enjin.sdk.services.notification.NotificationListener {

    private SpigotBootstrap bootstrap;

    public EnjEventListener(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void notificationReceived(NotificationEvent event) {
        try {
            bootstrap.debug(event.getData());
            NotificationType eventType = event.getType();

            if (eventType == null)
                return;

            switch (eventType) {
                case TX_EXECUTED:
                    onTxExecuted(event.getEvent());
                    break;
                case TXR_CANCELED_USER:
                case TXR_CANCELED_PLATFORM:
                    onTxrCancelled(event.getEvent());
                    break;
                case IDENTITY_LINKED:
                    onIdentityLinked(event.getEvent());
                    break;
                case IDENTITY_UPDATED:
                    onIdentityUpdated(event.getEvent());
                    break;
                case BALANCE_UPDATED:
                    onBalanceUpdated(event.getEvent());
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            bootstrap.log(ex);
        }
    }

    private void onTxExecuted(Event event) {
        EventData data = event.getData();
        TransactionType type = data.getRequestType();

        switch (type) {
            case CREATE_TRADE:
                onCreateTrade(data);
                break;
            case COMPLETE_TRADE:
                onCompleteTrade(data);
                break;
            default:
                break;
        }
    }

    private void onTxrCancelled(Event event) {
        EventData data = event.getData();
        TransactionType type = data.getRequestType();

        switch (type) {
            case CREATE_TRADE:
            case COMPLETE_TRADE:
                cancelTrade(data);
                break;
            default:
                break;
        }
    }

    private void onIdentityLinked(Event event) {
        EventData data = event.getData();
        if (data.getId() == null)
            return;

        EnjPlayer enjPlayer = bootstrap.getPlayerManager().getPlayer(data.getId()).orElse(null);
        if (enjPlayer == null)
            return;

        Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), enjPlayer::reloadIdentity);
    }

    private void onIdentityUpdated(Event event) {
        EventData data = event.getData();
        if (data.getId() == null)
            return;

        Optional<EnjPlayer> playerOptional = bootstrap.getPlayerManager().getPlayer(data.getId());
        playerOptional.ifPresent(player -> {
            if (data.getParam1() == null || data.getParam1().isEmpty())
                return;
            Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), player::unlinked);
        });
    }

    private void onBalanceUpdated(Event event) {
        String balanceRaw = event.getData().getParam4();
        String ethAddr = event.getData().getParam1();
        String tokenId = event.getData().getParam2();
        // Sets balance to zero if parameter is empty
        Integer balance = balanceRaw.isEmpty() ? Integer.valueOf(0) : Integer.parseInt(event.getData().getParam4());

        EnjPlayer enjPlayer = bootstrap.getPlayerManager().getPlayer(ethAddr).orElse(null);
        TokenModel tokenModel = bootstrap.getTokenManager().getToken(tokenId);

        if (enjPlayer == null || enjPlayer.getTokenWallet() == null)
            return;

        MutableBalance mBalance = enjPlayer.getTokenWallet().getBalance(tokenId);

        if (mBalance == null && balance > 0) {
            mBalance = new MutableBalance(tokenId, event.getData().getParam3(), balance);
            enjPlayer.getTokenWallet().setBalance(mBalance);

            // Adds the token's permissions to the player
            for (Map.Entry<String, List<String>> entry : tokenModel.getAssignablePermissions().entrySet()) {
                String world = entry.getKey();
                List<String> perms = entry.getValue();

                perms.forEach(perm -> { enjPlayer.addPermission(perm, tokenId, world); });
            }
        } else if (mBalance != null) {
            if (balance > 0) {
                mBalance.set(balance);
            } else {
                enjPlayer.getTokenWallet().removeBalance(tokenId);

                // Removes the token's permissions from the player
                for (Map.Entry<String, List<String>> entry : tokenModel.getAssignablePermissions().entrySet()) {
                    String world = entry.getKey();
                    List<String> perms = entry.getValue();

                    perms.forEach(perm -> { enjPlayer.removePermission(perm, world); });
                }
            }
        }

        enjPlayer.validateInventory();
    }

    private void onCreateTrade(EventData data) {
        if (data.getId() == null)
            return;
        String tradeId = data.getParam1();
        bootstrap.getTradeManager().sendCompleteRequest(data.getId(), tradeId);
    }

    private void onCompleteTrade(EventData data) {
        if (data.getId() == null)
            return;
        bootstrap.getTradeManager().completeTrade(data.getId());
    }

    private void cancelTrade(EventData data) {
        if (data.getId() == null)
            return;
        bootstrap.getTradeManager().cancelTrade(data.getId());
    }

}
