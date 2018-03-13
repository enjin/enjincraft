package io.enjincoin.spigot_framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.enjincoin.spigot_framework.commands.RootCommand;
import io.enjincoin.spigot_framework.controllers.SdkClientController;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SpigotBootstrap extends PluginBootstrap {

    private final BasePlugin main;
    private SdkClientController sdkClientController;

    public SpigotBootstrap(BasePlugin main) {
        this.main = main;
    }

    @Override
    public void setUp() {
        // Load the config to ensure that it is created or already exists.
        JsonObject config = getConfig();

        if (config != null) {
            this.sdkClientController = new SdkClientController(this.main, config);
            this.sdkClientController.setUp();
        }

        // Register Commands
        this.main.getCommand("enj").setExecutor(new RootCommand(this.main));
    }

    @Override
    public void tearDown() {
        this.sdkClientController.tearDown();
        this.sdkClientController = null;
    }

    @Override
    public SdkClientController getSdkController() {
        return this.sdkClientController;
    }

    @Override
    public JsonObject getConfig() {
        File file = new File(this.main.getDataFolder(), "config.json");
        JsonElement element = null;

        try {
            if (!file.exists())
                this.main.saveResource(file.getName(), false);

            JsonParser parser = new JsonParser();
            element = parser.parse(new FileReader(file));

            if (!(element instanceof JsonObject)) {
                boolean deleted = file.delete();
                if (deleted)
                    element = getConfig();
                else
                    this.main.getLogger().warning("An error occurred while load the configuration file.");
            }
        } catch (IOException ex) {
            this.main.getLogger().warning("An error occurred while creating the configuration file.");
        }

        return element.getAsJsonObject();
    }

}
