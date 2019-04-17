package com.enjin.enjincoin.spigot_framework;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * <p>Basic implementation of a Spigot plugin that initializes
 * the Spigot bootstrap.</p>
 *
 * @since 1.0
 */
public class BasePlugin extends JavaPlugin {

    /**
     * <p>The Spigot bootstrap.</p>
     */
    private SpigotBootstrap bootstrap;

    @Override
    public void onEnable() {
        // Initialize Spigot bootstrap.
        this.bootstrap = new SpigotBootstrap(this);
        this.bootstrap.setUp();
    }

    @Override
    public void onDisable() {
        // Clean up Spigot bootstrap.
        this.bootstrap.tearDown();
        this.bootstrap = null;
    }

    /**
     * <p>Returns the Spigot bootstrap.</p>
     *
     * @return the bootstrap
     * @since 1.0
     */
    public SpigotBootstrap getBootstrap() {
        return this.bootstrap;
    }
}
