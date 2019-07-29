package com.enjin.ecmp.spigot_framework.listeners;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.Messages;
import com.enjin.ecmp.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.sdk.model.service.notifications.NotificationEvent;
import com.enjin.enjincoin.sdk.model.service.notifications.NotificationType;
import com.enjin.enjincoin.sdk.service.notifications.NotificationListener;
import com.enjin.ecmp.spigot_framework.player.MinecraftPlayer;
import com.enjin.ecmp.spigot_framework.trade.TradeManager;
import com.google.gson.*;
import org.bukkit.Bukkit;

import java.math.BigInteger;

public class EnjinCoinEventListener implements NotificationListener {

    private Gson gson = new GsonBuilder().create();

    private BasePlugin plugin;

    public EnjinCoinEventListener(BasePlugin plugin) {
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
                    onTxExecuted(event);
                    break;
                case IDENTITY_LINKED:
                    onIdentityUpdated(event);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            new Exception("An error occurred while processing an sdk event.", e).printStackTrace();
        }
    }

    private void onTxExecuted(NotificationEvent event) {
        JsonObject source = gson.fromJson(event.getData(), JsonObject.class);
        JsonObject data = source.get("data").getAsJsonObject();

        if (data == null || !data.isJsonObject()) return;

        if (data.has("event")) {
            String txEventType = data.get("event").getAsString();

            if (txEventType == null) return;

            switch (txEventType) {
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
    }

    private void onIdentityUpdated(NotificationEvent event) {
        JsonObject source = gson.fromJson(event.getData(), JsonObject.class);
        JsonObject data = source.get("data").getAsJsonObject();

        if (data.has("id")) {
            BigInteger id = new BigInteger(data.get("id").getAsString());

            PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
            MinecraftPlayer mcPlayer = playerManager.getPlayer(id);

            if (mcPlayer != null) {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> mcPlayer.reloadUser());
            }
        }
    }

    private void onCreateTrade(JsonObject data) {
        String requestId = data.get("transaction_id").getAsString();
        String tradeId = data.get("param1").getAsString();
        TradeManager manager = this.plugin.getBootstrap().getTradeManager();
        manager.submitCompleteTrade(requestId, tradeId);
    }

    private void onCompleteTrade(JsonObject data) {
        String requestId = data.get("transaction_id").getAsString();
        TradeManager manager = this.plugin.getBootstrap().getTradeManager();
        manager.completeTrade(requestId);
    }

    private void onTransfer(JsonObject data) {
        if (!isObject(data.get("token"))) return;

        JsonObject token = data.get("token").getAsJsonObject();

        JsonElement fromEthAddr = token.get("param1");
        JsonElement toEthAddr = token.get("param2");

        if (!isPrimitive(fromEthAddr) || !isPrimitive(toEthAddr)) return;

        String name = getAsString(token.get("name"));
        String amount = getAsString(data.get("param4"));

        PlayerManager playerManager = this.plugin.getBootstrap().getPlayerManager();
        MinecraftPlayer fromMcPlayer = playerManager.getPlayer(fromEthAddr.getAsString());
        MinecraftPlayer toMcPlayer = playerManager.getPlayer(toEthAddr.getAsString());

        if (fromMcPlayer != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> fromMcPlayer.reloadUser());
            Messages.tokenSent(fromMcPlayer.getBukkitPlayer(), amount, name);
        }

        if (toMcPlayer != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> toMcPlayer.reloadUser());
            Messages.tokenReceived(toMcPlayer.getBukkitPlayer(), amount, name);
        }
    }

    private boolean isNull(JsonElement element) {
        return element == null || element.isJsonNull();
    }

    private boolean isObject(JsonElement element) {
        return !isNull(element) && element.isJsonObject();
    }

    private boolean isPrimitive(JsonElement element) {
        return !isNull(element) && element.isJsonPrimitive();
    }

    private String getAsString(JsonElement element) {
        if (!isNull(element)) {
            return element.getAsString();
        } else {
            return "n/a";
        }
    }

}
