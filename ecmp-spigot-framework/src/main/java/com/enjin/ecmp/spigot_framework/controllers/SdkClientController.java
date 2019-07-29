package com.enjin.ecmp.spigot_framework.controllers;

import com.enjin.ecmp.spigot_framework.BasePlugin;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.auth.AuthResult;
import com.enjin.enjincoin.sdk.model.service.platform.PlatformDetails;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;
import com.enjin.enjincoin.sdk.service.notifications.PusherNotificationService;
import com.enjin.ecmp.spigot_framework.listeners.EnjinCoinEventListener;
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
     * Config key for the base url of the trusted platform.
     */
    public static final String PLATFORM_BASE_URL = "platformBaseUrl";

    /**
     * Config key for the app id to use with the trusted platform.
     */
    public static final String APP_ID = "appId";

    /**
     * Config key for the secret to use with the trusted platform.
     */
    public static final String SECRET = "secret";

    /**
     * Config key for the UUID in an individual users session entry.
     */
    public static final String UUID = "uuid";

    private final BasePlugin plugin;

    private final JsonObject config;

    private TrustedPlatformClient client;
    private NotificationsService notificationsService;

    /**
     * <p>Controller constructor.</p>
     *
     * @param plugin   the Spigot plugin
     * @param config the bootstrap config
     */
    public SdkClientController(BasePlugin plugin, JsonObject config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * <p>Initialization mechanism for this controller.</p>
     *
     * @since 1.0
     */
    public void setUp() throws IOException {
        if (!config.has(PLATFORM_BASE_URL))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", PLATFORM_BASE_URL));
        if (!config.has(APP_ID))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", APP_ID));
        if (!config.has(SECRET))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", APP_ID));

        final String url = this.config.get(PLATFORM_BASE_URL).getAsString();
        final int appId = this.config.get(APP_ID).getAsInt();
        final String secret = this.config.get(SECRET).getAsString();

        this.client = new TrustedPlatformClient.Builder()
                .httpLogLevel(this.plugin.getBootstrap().isSDKDebuggingEnabled() ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE)
                .baseUrl(url)
                .readTimeout(1, TimeUnit.MINUTES)
                .build();
        HttpResponse<AuthResult> authResult = this.client.authAppSync(appId, secret);

        if (!authResult.isSuccess()) {
            this.plugin.getLogger().warning("Unable to authenticate your app credentials. " +
                    "Please check that config has the correct app id and secret.");
        }

        HttpResponse<GraphQLResponse<PlatformDetails>> platformResponse = this.client.getPlatformService().getPlatformSync();

        if (!platformResponse.isSuccess() || !platformResponse.body().isSuccess()) {
            this.plugin.getLogger().warning("Could not get platform details.");
        } else {
            PlatformDetails details = platformResponse.body().getData();

            this.notificationsService = new PusherNotificationService(details);
            this.notificationsService.start();

            this.notificationsService.registerListener(new EnjinCoinEventListener(this.plugin));
        }
    }

    /**
     * <p>Cleanup mechanism for this controller.</p>
     *
     * @since 1.0
     */
    public void tearDown() {
        try {
            this.client.close();
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.WARNING, "An error occurred while shutting down the Enjin Coin client.", e);
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
