package com.enjin.enjincraft.spigot.util;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class MaterialUtils {

    private static final String RAIL_SUFFIX = "RAIL";

    private static final String MINECART_SUFFIX = "MINECART";

    public static boolean isRail(Block block) {
       return isRail(block.getType());
    }

    public static boolean isRail(Material material) {
        return material.name().endsWith(RAIL_SUFFIX);
    }

    public static boolean isMinecart(Material material) {
        return material.name().endsWith(MINECART_SUFFIX);
    }

}
