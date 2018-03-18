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
                    Identity identity = getIdentity(ethereumAddress);

                    if (identity != null)
                        addTokenValue(identity, tokenId, -amount);
                }
            } else if (data.get("event").getAsString().equalsIgnoreCase("transfer")) {
                String fromEthereumAddress = data.get("param1").getAsString();
                String toEthereumAddress = data.get("param2").getAsString();
                double amount = Double.valueOf(data.get("param3").getAsString());
                int tokenId = data.get("token").getAsJsonObject().get("token_id").getAsInt();
                int appId = data.get("token").getAsJsonObject().get("app_id").getAsInt();

                this.main.getBootstrap().debug(String.format("%s received %s of %s tokens from %s", toEthereumAddress, amount, tokenId, fromEthereumAddress));

                JsonObject config = this.main.getBootstrap().getConfig();
                if (config.get("appId").getAsInt() == appId) {
                    this.main.getBootstrap().debug(String.format("Updating balance of player linked to %s", toEthereumAddress));
                    Identity toIdentity = getIdentity(toEthereumAddress);
                    Identity fromIdentity = getIdentity(fromEthereumAddress);

                    if (toIdentity != null)
                        addTokenValue(toIdentity, tokenId, amount);
                    if (fromEthereumAddress != null)
                        addTokenValue(fromIdentity, tokenId, -amount);
                }
            }
        }
    }

    public Identity getIdentity(String address) {
        return this.main.getBootstrap().getIdentities().values().stream()
                .filter(i -> i != null && i.getEthereumAddress().equalsIgnoreCase(address))
                .findFirst()
                .orElse(null);
    }

    public TokenEntry getTokenEntry(Identity identity, int tokenId) {
        TokenEntry entry = null;
        for (TokenEntry e : identity.getTokens()) {
            if (e.getTokenId() == tokenId) {
                entry = e;
                break;
            }
        }
        return entry;
    }

    public void addTokenValue(Identity identity, int tokenId, double amount) {
        TokenEntry entry = getTokenEntry(identity, tokenId);
        if (entry != null)
            entry.setValue(entry.getValue() + amount);
    }

}
