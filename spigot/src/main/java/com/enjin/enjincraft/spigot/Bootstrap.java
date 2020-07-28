package com.enjin.enjincraft.spigot;

import com.enjin.enjincraft.spigot.configuration.Conf;
import com.enjin.enjincraft.spigot.token.TokenManager;
import com.enjin.enjincraft.spigot.player.PlayerManager;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.services.notification.NotificationsService;

public interface Bootstrap {

    TrustedPlatformClient getTrustedPlatformClient();

    NotificationsService getNotificationsService();

    PlayerManager getPlayerManager();

    Conf getConfig();

    TokenManager getTokenManager();

}
