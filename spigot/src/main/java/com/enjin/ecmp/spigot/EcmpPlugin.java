package com.enjin.ecmp.spigot;

import org.bukkit.plugin.java.JavaPlugin;

public class EcmpPlugin extends JavaPlugin {

    private SpigotBootstrap bootstrap;

    @Override
    public void onEnable() {
        EcmpSpigot.register(bootstrap = new SpigotBootstrap(this));
        bootstrap.setUp();
    }

    @Override
    public void onDisable() {
        bootstrap.tearDown();
        EcmpSpigot.unregister();
    }

    public SpigotBootstrap bootstrap() {
        return bootstrap;
    }
}
