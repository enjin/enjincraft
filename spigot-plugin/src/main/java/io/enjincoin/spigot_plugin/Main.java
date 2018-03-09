package io.enjincoin.spigot_plugin;

import io.enjincoin.sdk.client.config.Config;
import io.enjincoin.spigot_framework.BasePlugin;

import java.io.File;

/**
 * A standalone Enjin Coin Spigot plugin.
 */
public class Main extends BasePlugin {

    @Override
    public String getBaseUrl() {
        return "https://enjin.v16studios.co.uk/";
    }

}
