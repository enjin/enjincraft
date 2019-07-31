package com.enjin.ecmp.spigot_framework.listeners;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.sdk.model.service.notifications.Event;
import com.enjin.enjincoin.sdk.model.service.notifications.EventData;
import com.enjin.enjincoin.sdk.model.service.notifications.NotificationEvent;
import com.enjin.enjincoin.sdk.model.service.notifications.NotificationType;
import com.enjin.ecmp.spigot_framework.player.EnjinCoinPlayer;
import com.enjin.ecmp.spigot_framework.trade.TradeManager;
import com.enjin.java_commons.StringUtils;
import org.bukkit.Bukkit;

public class NotificationListener implements com.enjin.enjincoin.sdk.service.notifications.NotificationListener {

    private BasePlugin plugin;

    public NotificationListener(BasePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void notificationReceived(NotificationEvent event) {
        try {
            NotificationType eventType = event.getType();
            plugin.getBootstrap().debug(String.format("Received event type %s on channel %s.", eventType, event.getChannel()));

            if (eventType == null) return;

            switch (eventType) {
                case TX_EXECUTED:
                    onTxExecuted(event.getEvent());
                    break;
                case IDENTITY_LINKED:
                    onIdentityUpdated(event.getEvent());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            new Exception("An error occurred while processing an sdk event.", e).printStackTrace();
        }
    }

    private void onTxExecuted(Event event) {
        EventData data = event.getData();
        String type = event.getEventType();

        if (StringUtils.isEmpty(type)) return;


        switch (type) {
            case "CreateTrade":
                onCreateTrade(data);
                break;
            case "CompleteTrade":
                onCompleteTrade(data);
                break;
            case "Transfer":
                onTransfer(data);
                break;
            default:
                break;
        }
    }

    private void onIdentityUpdated(Event event) {
        EventData data = event.getData();

        if (data.getId() != null) {
            PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
            EnjinCoinPlayer mcPlayer = playerManager.getPlayer(data.getId());

            if (mcPlayer != null) {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> mcPlayer.reloadIdentity());
            }
        }
    }

    private void onCreateTrade(EventData data) {
        String requestId = data.getTransactionId();
        String tradeId = data.getParam1();
        if (StringUtils.isEmpty(requestId) || StringUtils.isEmpty(tradeId)) return;
        TradeManager manager = this.plugin.getBootstrap().getTradeManager();
        manager.submitCompleteTrade(requestId, tradeId);
    }

    private void onCompleteTrade(EventData data) {
        String requestId = data.getTransactionId();
        if (StringUtils.isEmpty(requestId)) return;
        TradeManager manager = this.plugin.getBootstrap().getTradeManager();
        manager.completeTrade(requestId);
    }

    private void onTransfer(EventData data) {
        String fromEthAddr = data.getParam1();
        String toEthAddr = data.getParam2();

        if (StringUtils.isEmpty(fromEthAddr) || StringUtils.isEmpty(toEthAddr)) return;

        String amount = data.getParam4();

        PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
        EnjinCoinPlayer fromMcPlayer = playerManager.getPlayer(fromEthAddr);
        EnjinCoinPlayer toMcPlayer = playerManager.getPlayer(toEthAddr);

        if (fromMcPlayer != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> fromMcPlayer.reloadIdentity());
//            Messages.tokenSent(fromMcPlayer.getBukkitPlayer(), amount, name);
        }

        if (toMcPlayer != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> toMcPlayer.reloadIdentity());
//            Messages.tokenReceived(toMcPlayer.getBukkitPlayer(), amount, name);
        }
    }

}
