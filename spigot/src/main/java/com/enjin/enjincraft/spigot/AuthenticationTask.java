package com.enjin.enjincraft.spigot;

import org.bukkit.scheduler.BukkitRunnable;

public class AuthenticationTask extends BukkitRunnable {

    private SpigotBootstrap bootstrap;

    public AuthenticationTask(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void run() {
        bootstrap.authenticateTPClient();
    }

}
