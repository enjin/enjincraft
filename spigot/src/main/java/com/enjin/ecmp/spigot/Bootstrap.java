package com.enjin.ecmp.spigot;

import com.enjin.ecmp.spigot.configuration.Conf;
import com.enjin.ecmp.spigot.configuration.TokenConf;
import com.enjin.ecmp.spigot.player.PlayerManagerApi;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;

public interface Bootstrap {

    TrustedPlatformClient getTrustedPlatformClient();

    NotificationsService getNotificationsService();

    PlayerManagerApi getPlayerManager();

    Conf getConfig();

    TokenConf getTokenConf();

}
