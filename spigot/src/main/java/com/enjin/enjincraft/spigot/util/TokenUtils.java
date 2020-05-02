package com.enjin.enjincraft.spigot.util;

import com.enjin.enjincraft.spigot.token.TokenModel;
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

    public static boolean canCombineStacks(ItemStack first, ItemStack second) {
        String firstId = getTokenID(first);
        String secondId = getTokenID(second);

        if (StringUtils.isEmpty(firstId) || StringUtils.isEmpty(secondId) || !firstId.equals(secondId))
            return false;

        int maxStackSize = first.getMaxStackSize();
        return maxStackSize == second.getMaxStackSize()
                && first.getType() == second.getType()
                && first.getAmount() + second.getAmount() <= maxStackSize;
    }

}
