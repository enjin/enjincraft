package com.enjin.ecmp.spigot_framework.controllers;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.ecmp.spigot_framework.ConfigKeys;
import com.enjin.ecmp.spigot_framework.SpigotBootstrap;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.auth.AuthResult;
import com.enjin.enjincoin.sdk.model.service.platform.PlatformDetails;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;
import com.enjin.enjincoin.sdk.service.notifications.PusherNotificationService;
import com.enjin.ecmp.spigot_framework.listeners.EnjinCoinEventListener;
import com.enjin.java_commons.StringUtils;
import com.google.gson.JsonObject;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * <p>Controller for the Enjin Coin SDK client.</p>
 *
 * @since 1.0
 */
public class SdkClientController {

    /**
     * Config key for the UUID in an individual users session entry.
     */
    public static final String UUID = "uuid";

    private SpigotBootstrap bootstrap;

    private TrustedPlatformClient client;
    private NotificationsService notificationsService;

    /**
     * <p>Controller constructor.</p>
     *
     * @param bootstrap the spigot bootstrap
     */
    public SdkClientController(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    /**
     * <p>Initialization mechanism for this controller.</p>
     *
     * @since 1.0
     */
    public void setUp() throws IOException {
        if (StringUtils.isEmpty(bootstrap.getConfig().getPlatformBaseUrl()))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config or the value is invalid.", ConfigKeys.BASE_URL));
        if (bootstrap.getConfig().getAppId() == -1)
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config or the value is invalid.", ConfigKeys.APP_ID));
        if (StringUtils.isEmpty(bootstrap.getConfig().getAppSecret()))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config or the value is invalid.", ConfigKeys.APP_SECRET));

        client = new TrustedPlatformClient.Builder()
                .httpLogLevel(bootstrap.getConfig().isSdkDebugging() ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE)
                .baseUrl(bootstrap.getConfig().getPlatformBaseUrl())
                .readTimeout(1, TimeUnit.MINUTES)
                .build();
        HttpResponse<AuthResult> authResult = client.authAppSync(bootstrap.getConfig().getAppId(), bootstrap.getConfig().getAppSecret());

        if (!authResult.isSuccess()) {
            bootstrap.getLogger().warning("Unable to authenticate your app credentials. " +
                    "Please check that config has the correct app id and secret.");
        }

        HttpResponse<GraphQLResponse<PlatformDetails>> platformResponse = client.getPlatformService().getPlatformSync();

        if (!platformResponse.isSuccess() || !platformResponse.body().isSuccess()) {
            bootstrap.getLogger().warning("Could not get platform details.");
        } else {
            PlatformDetails details = platformResponse.body().getData();

            notificationsService = new PusherNotificationService(details);
            notificationsService.start();

            notificationsService.registerListener(new EnjinCoinEventListener(bootstrap.getPlugin()));
        }
    }

    /**
     * <p>Cleanup mechanism for this controller.</p>
     *
     * @since 1.0
     */
    public void tearDown() {
        try {
            client.close();
        } catch (IOException e) {
            bootstrap.getLogger().log(Level.WARNING, "An error occurred while shutting down the Enjin Coin client.", e);
        }
    }

    /**
     * <p>Returns the Enjin Coin SDK client.</p>
     *
     * @return the client or null if not initialized
     * @since 1.0
     */
    public TrustedPlatformClient getClient() {
        return client;
    }

    public NotificationsService getNotificationsService() {
        return notificationsService;
    }
}
