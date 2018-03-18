package io.enjincoin.spigot_framework;

import com.google.gson.JsonObject;
import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.spigot_framework.controllers.SdkClientController;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for a bootstrapped application or plugin.
 */
public interface Bootstrap {

    void setUp();

    void tearDown();

    SdkClientController getSdkController();

    Map<UUID, Identity> getIdentities();

    JsonObject getConfig();

    boolean isDebugEnabled();

    void debug(String log);

}
