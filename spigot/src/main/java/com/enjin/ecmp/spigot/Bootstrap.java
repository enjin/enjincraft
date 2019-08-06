package com.enjin.ecmp.spigot;

import com.enjin.ecmp.spigot.configuration.EcmpConfig;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.trade.TradeManager;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;

import java.util.logging.Logger;

public interface Bootstrap {

    TrustedPlatformClient getTrustedPlatformClient();

    NotificationsService getNotificationsService();

    PlayerManager getPlayerManager();

    TradeManager getTradeManager();

    EcmpConfig getConfig();

}
