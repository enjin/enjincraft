package com.enjin.enjincraft.spigot;

import com.enjin.enjincraft.spigot.configuration.Conf;
import com.enjin.enjincraft.spigot.configuration.TokenConf;
import com.enjin.enjincraft.spigot.player.PlayerManagerApi;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;

public interface Bootstrap {

    TrustedPlatformClient getTrustedPlatformClient();

    NotificationsService getNotificationsService();

    PlayerManagerApi getPlayerManager();

    Conf getConfig();

    TokenConf getTokenConf();

}
