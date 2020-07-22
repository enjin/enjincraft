package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.google.gson.*;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyTokenConverter {

    public static final String BACKUP_EXT = ".backup";
    public static final String FILE_NAME = "tokens.json";
    @Deprecated
    public static final String FILE_BACKUP_NAME = "tokens.json.backup";
    public static final String TOKENS = "tokens";
    public static final String ITEM_NAME_KEY = "displayName";
    public static final String ITEM_MATERIAL_KEY = "material";
    public static final String ITEM_LORE_KEY = "lore";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(TokenPermission.class, new TokenPermission.TokenPermissionDeserializer())
            .create();
    private final SpigotBootstrap bootstrap;
    private final File file;

    public LegacyTokenConverter(SpigotBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.file = new File(bootstrap.plugin().getDataFolder(), FILE_NAME);
    }

    public void process() {
        processTokenDir();
        processDataFolderFiles();
    }

    private void processTokenDir() {
        TokenManager tokenManager = bootstrap.getTokenManager();

        File dir = tokenManager.getDir();
        if (!dir.exists())
            return;

        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(TokenManager.JSON_EXT))
                continue;

            try (InputStreamReader in = new InputStreamReader(new FileInputStream(file), TokenManager.CHARSET)) {
                tokenManager.saveToken(gson.fromJson(in, TokenModel.class));
            } catch (Exception e) {
                bootstrap.log(e);
            } finally {
                try {
                    if (!createBackup(file))
                        throw new Exception(String.format("Unable to setup legacy tokens backup file in \"%s\" for %s",
                                file.getParent(),
                                file.getName()));
                } catch (Exception e) {
                    bootstrap.log(e);
                }
            }
        }
    }

    private void processDataFolderFiles() {
        if (!file.exists())
            return;

        try (FileReader fr = new FileReader(file)) {
            JsonElement element = gson.fromJson(fr, JsonElement.class);
            if (!element.isJsonObject())
                return;

            JsonObject root = element.getAsJsonObject();
            if (!root.has(TOKENS))
                return;

            element = root.get(TOKENS);
            if (!element.isJsonObject())
                return;

            TokenManager tokenManager = bootstrap.getTokenManager();
            Map<String, JsonObject> tokens = new HashMap<>();

            // Readies the data for conversion
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String tokenId = entry.getKey();
                JsonElement tokenDef = entry.getValue();

                // Ignores existing tokens and non-JSON objects
                if (tokenManager.hasToken(tokenId) || !tokenDef.isJsonObject())
                    continue;

                tokens.put(tokenId, tokenDef.getAsJsonObject());
            }

            convert(tokens);
        } catch (Exception e) {
            bootstrap.log(e);
        } finally {
            try {
                if (!createBackup(file))
                    throw new Exception(String.format("Unable to setup legacy tokens backup file in \"%s\"", file.getParent()));
            } catch (Exception e) {
                bootstrap.log(e);
            }
        }
    }

    private void convert(Map<String, JsonObject> tokens) {
        TokenManager tokenManager = bootstrap.getTokenManager();

        for (Map.Entry<String, JsonObject> entry : tokens.entrySet()) {
            String tokenId = entry.getKey();
            JsonObject json = entry.getValue();

            Material material = getItemMaterial(json);
            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (json.has(ITEM_NAME_KEY))
                    meta.setDisplayName(json.get(ITEM_NAME_KEY).getAsString());
                if (meta instanceof BookMeta)
                    setBookMeta(json, (BookMeta) meta);
                if (json.has(ITEM_LORE_KEY))
                    setItemLore(json, meta);

                itemStack.setItemMeta(meta);
            }

            NBTContainer nbt = NBTItem.convertItemtoNBT(itemStack);
            TokenModel tokenModel = TokenModel.builder()
                    .id(tokenId)
                    .alternateId(null)
                    .nbt(nbt.toString())
                    .build();

            tokenManager.saveToken(tokenModel);
        }
    }

    private Material getItemMaterial(JsonObject json) {
        String mat = json.get(ITEM_MATERIAL_KEY).getAsString();
        Material material = Material.getMaterial(mat);

        // If the material returned null try getting the material using legacy names
        if (material == null)
            material = Material.getMaterial(mat, true);

        // If the material returned null for both non-legacy and legacy names use an apple as material
        if (material == null)
            material = Material.APPLE;

        return material;
    }

    private void setBookMeta(JsonObject json, BookMeta meta) {
        if (json.has("title"))
            meta.setTitle(json.get("title").getAsString());
        if (json.has("author"))
            meta.setAuthor(json.get("author").getAsString());
        if (json.has("pages")) {
            for (JsonElement page : json.getAsJsonArray("pages"))
                meta.addPage(page.getAsString());
        }
    }

    private void setItemLore(JsonObject json, ItemMeta meta) {
        List<String> lore = new ArrayList<>();

        JsonElement loreElem = json.get(ITEM_LORE_KEY);
        if (loreElem.isJsonArray()) {
            JsonArray loreArray = loreElem.getAsJsonArray();
            for (JsonElement line : loreArray) {
                lore.add(ChatColor.DARK_GRAY + line.getAsString());
            }
        } else {
            lore.add(ChatColor.DARK_GRAY + loreElem.getAsString());
        }

        meta.setLore(lore);
    }

    public boolean fileExists() {
        return file.exists();
    }

    private static boolean createBackup(File file) {
        int count = 0;
        String newName;
        File other;
        do {
            newName = count == 0
                    ? file.getName()
                    : String.format("%s (%d)", file.getName(), count);
            other = new File(file.getParent(), newName);
            count++;
        } while (other.exists());

        return file.renameTo(new File(file.getParent(), String.format("%s%s", newName, BACKUP_EXT)));
    }

}
