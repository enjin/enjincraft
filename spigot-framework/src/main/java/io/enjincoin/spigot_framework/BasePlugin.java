package io.enjincoin.spigot_framework;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class BasePlugin extends JavaPlugin {

    private SpigotBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new SpigotBootstrap(this);
        this.bootstrap.setUp();
    }

    @Override
    public void onDisable() {
        this.bootstrap.tearDown();
        this.bootstrap = null;
    }

    public SpigotBootstrap getBootstrap() {
        return this.bootstrap;
    }

    public abstract String getBaseUrl();
}
