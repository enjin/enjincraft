package com.enjin.ecmp.spigot;

import java.util.Optional;

public class ECMP {

    private static Optional<SpigotBootstrap> instance = Optional.empty();

    protected static void register(SpigotBootstrap instance) {
        ECMP.instance = Optional.ofNullable(instance);
    }

    protected static void unregister() {
        instance = Optional.empty();
    }

    public static boolean isRegistered() {
        return instance.isPresent();
    }

    public static Optional<? extends Bootstrap> bootstrap() {
        return instance;
    }

    protected static Optional<? extends Module> module() {
        return instance;
    }
}
