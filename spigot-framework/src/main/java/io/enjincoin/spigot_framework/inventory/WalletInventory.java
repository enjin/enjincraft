package io.enjincoin.spigot_framework.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.sdk.client.service.identity.vo.TokenEntry;
import io.enjincoin.sdk.client.service.tokens.vo.Token;
import io.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class WalletInventory {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public static Inventory create(BasePlugin main, InventoryHolder holder, Identity identity) {
        JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();
        Inventory inventory = Bukkit.createInventory(holder, 6 * 9, ChatColor.DARK_PURPLE + "Enjin Wallet");
        int index = 0;
        for (TokenEntry entry : identity.getTokens()) {
            if (index >= 6 * 9)
                break;

            if (entry.getValue() > 0) {
                Token token = main.getBootstrap().getTokens().get(entry.getTokenId());
                if (token == null)
                    continue;

                JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(token.getTokenId()))
                        ? tokensDisplayConfig.get(String.valueOf(token.getTokenId())).getAsJsonObject()
                        : null;

                Material material = null;
                if (tokenDisplay != null && tokenDisplay.has("material"))
                    material = Material.getMaterial(tokenDisplay.get("material").getAsString());
                if (material == null)
                    material = Material.APPLE;

                ItemStack stack = new ItemStack(material);
                ItemMeta meta = stack.getItemMeta();

                if (tokenDisplay != null && tokenDisplay.has("displayName")) {
                    meta.setDisplayName(ChatColor.DARK_PURPLE + tokenDisplay.get("displayName").getAsString());
                } else {
                    if (token.getName() != null)
                        meta.setDisplayName(ChatColor.DARK_PURPLE + token.getName());
                    else
                        meta.setDisplayName(ChatColor.DARK_PURPLE + "Token #" + token.getTokenId());
                }

                List<String> lore = new ArrayList<>();
                if (token.getDecimals() == 0) {
                    int balance = Double.valueOf(entry.getValue()).intValue();
                    lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + balance);
                } else {
                    double balance = entry.getValue();
                    lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + DECIMAL_FORMAT.format(balance));
                }

                if (tokenDisplay != null && tokenDisplay.has("lore")) {
                    JsonElement element = tokenDisplay.get("lore");
                    if (element.isJsonArray()) {
                        JsonArray array = element.getAsJsonArray();
                        for (JsonElement line : array) {
                            lore.add(ChatColor.DARK_GRAY + line.getAsString());
                        }
                    } else {
                        lore.add(ChatColor.DARK_GRAY + element.getAsString());
                    }
                }

                meta.setLore(lore);
                stack.setItemMeta(meta);

                inventory.setItem(index++, stack);
            }
        }
        return inventory;
    }

}
