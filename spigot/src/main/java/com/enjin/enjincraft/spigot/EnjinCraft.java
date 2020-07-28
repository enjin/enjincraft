package com.enjin.enjincraft.spigot;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class EnjinCraft {

    @Nullable
    private SpigotBootstrap instance;

    protected void register(SpigotBootstrap instance) {
        EnjinCraft.instance = instance;
    }

    protected void unregister() {
        instance = null;
    }

    public boolean isRegistered() {
        return instance != null;
    }

    public <T extends Bootstrap> T bootstrap() {
        return (T) instance;
    }
}
