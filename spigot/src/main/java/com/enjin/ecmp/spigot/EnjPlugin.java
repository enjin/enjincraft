package com.enjin.ecmp.spigot;

import org.bukkit.plugin.java.JavaPlugin;

public class EnjPlugin extends JavaPlugin {

    private SpigotBootstrap bootstrap;

    @Override
    public void onEnable() {
        EnjSpigot.register(bootstrap = new SpigotBootstrap(this));
        bootstrap.setUp();
    }

    @Override
    public void onDisable() {
        bootstrap.tearDown();
        EnjSpigot.unregister();
    }

    public SpigotBootstrap bootstrap() {
        return bootstrap;
    }
}
