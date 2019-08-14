package com.enjin.ecmp.spigot;

public class EnjSpigot {

    private static SpigotBootstrap bootstrap;

    protected static void register(SpigotBootstrap bootstrap) {
        EnjSpigot.bootstrap = bootstrap;
    }

    protected static void unregister() {
        bootstrap = null;
    }

    public static boolean isRegistered() {
        return bootstrap != null;
    }

    public static Bootstrap bootstrap() {
        return bootstrap;
    }

    protected static Module module() {
        return bootstrap;
    }
}
