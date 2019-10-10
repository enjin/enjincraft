package com.enjin.enjincraft.spigot.listeners;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.wallet.MutableBalance;
import com.enjin.enjincoin.sdk.model.service.notifications.*;
import com.enjin.enjincoin.sdk.model.service.requests.TransactionType;
import org.bukkit.Bukkit;

public class NotificationListener implements com.enjin.enjincoin.sdk.service.notifications.NotificationListener {

    private SpigotBootstrap bootstrap;

    public NotificationListener(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void notificationReceived(NotificationEvent event) {
        try {
            bootstrap.debug(event.getData());
            NotificationType eventType = event.getType();

            if (eventType == null) return;

            switch (eventType) {
                case TX_EXECUTED:
                    onTxExecuted(event.getEvent());
                    break;
                case TXR_CANCELED_USER:
                case TXR_CANCELED_PLATFORM:
                    onTxrCancelled(event.getEvent());
                    break;
                case IDENTITY_LINKED:
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

    private void onIdentityUpdated(Event event) {
        EventData data = event.getData();
        if (data.getId() == null) return;

        EnjPlayer enjPlayer = bootstrap.getPlayerManager().getPlayer(data.getId()).orElse(null);
        if (enjPlayer == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(bootstrap.plugin(), () -> enjPlayer.reloadIdentity());
    }

    private void onBalanceUpdated(Event event) {
        String ethAddr = event.getData().getParam1();
        String tokenId = event.getData().getParam2();
        Integer balance = Integer.parseInt(event.getData().getParam4());
        EnjPlayer enjPlayer = bootstrap.getPlayerManager().getPlayer(ethAddr).orElse(null);

        if (enjPlayer == null) return;

        MutableBalance mBalance = enjPlayer.getTokenWallet().getBalance(tokenId);

        if (mBalance == null) {
            mBalance = new MutableBalance(tokenId, event.getData().getParam3(), balance);
            enjPlayer.getTokenWallet().setBalance(mBalance);
        } else {
            mBalance.set(balance);
        }

        enjPlayer.validateInventory();
    }

    private void onCreateTrade(EventData data) {
        if (data.getId() == null) return;
        String tradeId = data.getParam1();
        bootstrap.getTradeManager().sendCompleteRequest(data.getId(), tradeId);
    }

    private void onCompleteTrade(EventData data) {
        if (data.getId() == null) return;
        bootstrap.getTradeManager().completeTrade(data.getId());
    }

    private void cancelTrade(EventData data) {
        if (data.getId() == null) return;
        bootstrap.getTradeManager().cancelTrade(data.getId());
    }

}
