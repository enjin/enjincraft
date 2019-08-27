package com.enjin.ecmp.spigot.listeners;

import com.enjin.ecmp.spigot.NotificationServiceException;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.trade.TradeManager;
import com.enjin.ecmp.spigot.wallet.MutableBalance;
import com.enjin.enjincoin.sdk.model.service.notifications.Event;
import com.enjin.enjincoin.sdk.model.service.notifications.EventData;
import com.enjin.enjincoin.sdk.model.service.notifications.NotificationEvent;
import com.enjin.enjincoin.sdk.model.service.notifications.NotificationType;
import com.enjin.java_commons.StringUtils;
import org.bukkit.Bukkit;

public class NotificationListener implements com.enjin.enjincoin.sdk.service.notifications.NotificationListener {

    private SpigotBootstrap bootstrap;

    public NotificationListener(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void notificationReceived(NotificationEvent event) {
        try {
            bootstrap.debug(event.toString());
            NotificationType eventType = event.getType();

            if (eventType == null) return;

            switch (eventType) {
                case TX_EXECUTED:
                    onTxExecuted(event.getEvent());
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
        String type = data.getRequestType();

        if (StringUtils.isEmpty(type)) return;

        switch (type) {
            case "create_trade":
                onCreateTrade(data);
                break;
            case "complete_trade":
                onCompleteTrade(data);
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
        String tradeId = data.getParam1();
        if (data.getId() == null) return;
        TradeManager manager = bootstrap.getTradeManager();
        manager.submitCompleteTrade(data.getId(), tradeId);
    }

    private void onCompleteTrade(EventData data) {
        if (data.getId() == null) return;
        TradeManager manager = bootstrap.getTradeManager();
        manager.completeTrade(data.getId());
    }

}
