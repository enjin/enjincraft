package com.enjin.enjincraft.spigot;

import com.enjin.enjincraft.spigot.configuration.Conf;
import com.enjin.enjincraft.spigot.player.PlayerManagerApi;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.services.notification.NotificationsService;

public interface Bootstrap {

    TrustedPlatformClient getTrustedPlatformClient();

    NotificationsService getNotificationsService();

    PlayerManagerApi getPlayerManager();

    Conf getConfig();

    TokenManager getTokenManager();

}
