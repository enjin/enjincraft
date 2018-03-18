package io.enjincoin.spigot_framework.listeners.notifications;

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
        this.main.getLogger().info(event.getNotificationType().name());
    }

}
