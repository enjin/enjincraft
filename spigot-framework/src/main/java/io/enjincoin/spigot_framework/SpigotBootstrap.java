package io.enjincoin.spigot_framework;

import io.enjincoin.spigot_framework.commands.RootCommand;

public class SpigotBootstrap extends PluginBootstrap {

    private final BasePlugin main;

    public SpigotBootstrap(BasePlugin main) {
        this.main = main;
    }

    @Override
    public void setUp() {
        // Register Commands
        this.main.getCommand("enj").setExecutor(new RootCommand(this.main));
    }

}
