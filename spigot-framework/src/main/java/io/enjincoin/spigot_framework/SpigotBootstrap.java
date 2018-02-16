package io.enjincoin.spigot_framework;

import io.enjincoin.spigot_framework.commands.RootCommand;
import io.enjincoin.spigot_framework.controllers.SdkClientController;

public class SpigotBootstrap extends PluginBootstrap {

    private final BasePlugin main;
    private SdkClientController sdkClientController;

    public SpigotBootstrap(BasePlugin main) {
        this.main = main;
    }

    @Override
    public void setUp() {
        this.sdkClientController = new SdkClientController(this.main);
        this.sdkClientController.setUp();

        // Register Commands
        this.main.getCommand("enj").setExecutor(new RootCommand(this.main));
    }

    @Override
    public void tearDown() {
        this.sdkClientController.tearDown();
        this.sdkClientController = null;
    }

    @Override
    public SdkClientController getSdkClientController() {
        return this.sdkClientController;
    }

}
