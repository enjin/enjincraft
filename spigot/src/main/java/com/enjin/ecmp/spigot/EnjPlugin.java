package com.enjin.ecmp.spigot;

import org.bukkit.plugin.java.JavaPlugin;

public class EnjPlugin extends JavaPlugin {

    private SpigotBootstrap bootstrap;

    @Override
    public void onEnable() {
        ECMP.register(bootstrap = new SpigotBootstrap(this));
        bootstrap.setUp();
    }

    @Override
    public void onDisable() {
        bootstrap.tearDown();
        ECMP.unregister();
    }

    public SpigotBootstrap bootstrap() {
        return bootstrap;
    }
}
