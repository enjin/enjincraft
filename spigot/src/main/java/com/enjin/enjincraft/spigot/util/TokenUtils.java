package com.enjin.enjincraft.spigot.util;

import com.enjin.enjincraft.spigot.configuration.TokenDefinition;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;

public class TokenUtils {

    private TokenUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getTokenID(ItemStack is) {
        if (is != null) {
            NBTItem nbt = new NBTItem(is);

            if (nbt.hasKey(TokenDefinition.NBT_ID))
                return nbt.getString(TokenDefinition.NBT_ID);
        }

        return null;
    }

}
