package com.enjin.enjincraft.spigot.util;

import com.enjin.enjincraft.spigot.configuration.TokenModel;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;

public class TokenUtils {

    private TokenUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String getTokenID(ItemStack is) {
        if (is != null) {
            NBTItem nbt = new NBTItem(is);

            if (nbt.hasKey(TokenModel.NBT_ID))
                return nbt.getString(TokenModel.NBT_ID);
        }

        return null;
    }

}
