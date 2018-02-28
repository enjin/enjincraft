package io.enjincoin.spigot_plugin;

import io.enjincoin.sdk.client.config.Config;
import io.enjincoin.spigot_framework.BasePlugin;

import java.io.File;

/**
 * A standalone Enjin Coin Spigot plugin.
 */
public class Main extends BasePlugin {

    @Override
    public Config getSdkConfig() {
        Config config = null;
        File file = new File(getDataFolder(), "sdk.json");

        try {
            config = Config.load(file);
        } catch (Exception e) {
            getLogger().warning(String.format("Unable to create or load configuration file at %s.", file.getPath()));
        }

        return config;
    }

}
