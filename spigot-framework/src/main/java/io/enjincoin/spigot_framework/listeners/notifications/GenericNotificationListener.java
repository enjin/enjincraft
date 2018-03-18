package io.enjincoin.spigot_framework.listeners.notifications;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.enjincoin.sdk.client.enums.NotificationType;
import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.sdk.client.service.identity.vo.TokenEntry;
import io.enjincoin.sdk.client.service.notifications.NotificationListener;
import io.enjincoin.sdk.client.vo.notifications.NotificationEvent;
import io.enjincoin.spigot_framework.BasePlugin;

public class GenericNotificationListener implements NotificationListener {

    private BasePlugin main;

    public GenericNotificationListener(BasePlugin main) {
        this.main = main;
    }

    @Override
    public void notificationReceived(NotificationEvent event) {
        this.main.getBootstrap().debug(String.format("Received %s event with data: %s", event.getNotificationType().getEventType(), event.getSourceData()));
        if (event.getNotificationType() == NotificationType.TX_EXECUTED) {
            this.main.getBootstrap().debug(String.format("Parsing data for %s event", event.getNotificationType().getEventType()));
            JsonParser parser = new JsonParser();
            JsonObject data = parser.parse(event.getSourceData()).getAsJsonObject()
                    .get("data").getAsJsonObject();
            if (data.get("event").getAsString().equalsIgnoreCase("melt")) {
                String ethereumAddress = data.get("param1").getAsString();
                double amount = Double.valueOf(data.get("param2").getAsString());
                int tokenId = data.get("token").getAsJsonObject().get("token_id").getAsInt();
                int appId = data.get("token").getAsJsonObject().get("app_id").getAsInt();

                this.main.getBootstrap().debug(String.format("%s of token %s was melted by %s", amount, tokenId, ethereumAddress));

                JsonObject config = this.main.getBootstrap().getConfig();
                if (config.get("appId").getAsInt() == appId) {
                    this.main.getBootstrap().debug(String.format("Updating balance of player linked to %s", ethereumAddress));
                    Identity identity = this.main.getBootstrap().getIdentities().values().stream()
                            .filter(i -> i != null && i.getEthereumAddress().equalsIgnoreCase(ethereumAddress))
                            .findFirst()
                            .orElse(null);

                    if (identity != null) {
                        this.main.getBootstrap().debug(String.format("Locating entry for token %s", tokenId));
                        for (TokenEntry entry : identity.getTokens()) {
                            if (entry.getTokenId() == tokenId) {
                                this.main.getBootstrap().debug(String.format("Setting amount of entry for token %s to %s", tokenId, entry.getValue() - amount));
                                entry.setValue(entry.getValue() - amount);
                            }
                        }
                    } else {
                        this.main.getBootstrap().debug(String.format("Could not locate identity linked to %s that is online.", ethereumAddress));
                    }
                } else {
                    this.main.getBootstrap().debug(String.format("Config is missing appId field or the value does not equal %s", appId));
                }
            } else {
                this.main.getBootstrap().debug(String.format("Event: %s" + data.get("event").getAsString()));
            }
        }
    }

}
