package com.enjin.enjincraft.spigot;

import java.util.Optional;

public class EnjinCraft {

    private static Optional<SpigotBootstrap> instance = Optional.empty();

    protected static void register(SpigotBootstrap instance) {
        EnjinCraft.instance = Optional.ofNullable(instance);
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
