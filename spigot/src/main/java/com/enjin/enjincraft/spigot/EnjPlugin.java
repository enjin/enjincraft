package com.enjin.enjincraft.spigot;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class EnjPlugin extends JavaPlugin {

    private SpigotBootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new SpigotBootstrap(this);
        EnjinCraft.register(bootstrap);
        bootstrap.setUp();
    }

    @Override
    public void onDisable() {
        bootstrap.tearDown();
        EnjinCraft.unregister();
    }

    public SpigotBootstrap bootstrap() {
        return bootstrap;
    }

    public void log(Throwable throwable) {
        getLogger().log(Level.WARNING, "Exception Caught", throwable);
    }
}
