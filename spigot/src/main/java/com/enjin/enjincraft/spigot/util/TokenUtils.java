package com.enjin.enjincraft.spigot.util;

import com.enjin.enjincraft.spigot.token.TokenModel;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TokenUtils {

    public static final String BASE_INDEX   = "0000000000000000";
    public static final int    INDEX_LENGTH = BASE_INDEX.length();
    public static final int    ID_LENGTH    = 16;

    private TokenUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isNonFungible(ItemStack is) {
        if (is != null && is.getType() != Material.AIR) {
            NBTItem nbtItem = new NBTItem(is);

            if (nbtItem.hasKey(TokenModel.NBT_NONFUNGIBLE))
                return nbtItem.getBoolean(TokenModel.NBT_NONFUNGIBLE);
        }

        return false;
    }

    public static String getTokenID(String fullId) throws IllegalArgumentException {
        if (!isValidFullId(fullId))
            throw new IllegalArgumentException("Invalid full ID");

        return fullId.substring(0, ID_LENGTH);
    }

    public static String getTokenID(ItemStack is) {
        if (is != null && is.getType() != Material.AIR) {
            NBTItem nbtItem = new NBTItem(is);

            if (nbtItem.hasKey(TokenModel.NBT_ID))
                return nbtItem.getString(TokenModel.NBT_ID);

            return "";
        }

        return null;
    }

    public static String getTokenIndex(String fullId) throws IllegalArgumentException {
        if (!isValidFullId(fullId))
            throw new IllegalArgumentException("Invalid full ID");

        return fullId.substring(ID_LENGTH);
    }

    public static String getTokenIndex(ItemStack is) {
        if (is != null && is.getType() != Material.AIR) {
            NBTItem nbtItem = new NBTItem(is);

            if (nbtItem.hasKey(TokenModel.NBT_INDEX))
                return nbtItem.getString(TokenModel.NBT_INDEX);

            return "";
        }

        return null;
    }

    public static boolean isValidFullId(String fullId) {
        return isValidString(fullId, ID_LENGTH + INDEX_LENGTH);
    }

    public static boolean isValidId(String id) {
        return isValidString(id, ID_LENGTH);
    }

    public static boolean isValidIndex(String index) {
        return isValidString(index, INDEX_LENGTH);
    }

    private static boolean isValidString(String s, int length) {
        if (s == null || s.length() != length)
            return false;

        for (char ch : s.toCharArray()) {
            if (!isValidCharacter(ch))
                return false;
        }

        return true;
    }

    private static boolean isValidCharacter(char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f');
    }

    public static String createFullId(@NonNull TokenModel tokenModel) {
        return createFullId(tokenModel.getId(), tokenModel.getIndex());
    }

    public static String createFullId(@NonNull String id) throws IllegalArgumentException {
        return createFullId(id, TokenUtils.BASE_INDEX);
    }

    public static String createFullId(@NonNull String id, String index) throws IllegalArgumentException {
        id    = formatId(id);
        index = index == null
                ? BASE_INDEX
                : formatIndex(index);
        return id + index;
    }

    public static String formatId(String id) throws IllegalArgumentException {
        if (id.length() != ID_LENGTH)
            throw new IllegalArgumentException("Provided string is not the ID length");

        char[] chars = new char[ID_LENGTH];
        for (int i = 0; i < ID_LENGTH; i++) {
            chars[i] = formatCharacter(id.charAt(i));
        }

        return new String(chars);
    }

    public static String formatIndex(String index) throws IllegalArgumentException {
        if (index.length() > INDEX_LENGTH)
            throw new IllegalArgumentException("Provided string is larger than the index length");

        char[] chars = new char[INDEX_LENGTH];
        for (int i = 0; i < INDEX_LENGTH; i++) {
            char ch = i < index.length()
                    ? formatCharacter(index.charAt(index.length() - i - 1))
                    : '0';
            chars[INDEX_LENGTH - i - 1] = ch;
        }

        return new String(chars);
    }

    private static char formatCharacter(char ch) throws IllegalArgumentException {
        if (ch >= 'A' && ch <= 'F')
            ch += 32; // To lowercase

        if (!isValidCharacter(ch))
            throw new IllegalArgumentException("Provided string is not in hexadecimal");

        return ch;
    }

    public static String trimIndex(String index) throws IllegalArgumentException {
        if (!isValidIndex(index))
            throw new IllegalArgumentException("Provided string is not a valid index");

        for (int i = 0; i < index.length(); i++) {
            if (index.charAt(i) != '0')
                return index.substring(i);
        }

        return "";
    }

    public static String toFullId(String id) {
        if (isValidId(id))
            return createFullId(id);
        else if (isValidFullId(id))
            return id;

        return null;
    }

    public static String normalizeFullId(String fullId) {
        if (!isValidFullId(fullId))
            return null;

        return createFullId(getTokenID(fullId));
    }

    public static Long convertIndexToLong(String index) throws IllegalArgumentException {
        if (!isValidIndex(index))
            throw new IllegalArgumentException("Provided string is not a valid index");

        return Long.parseLong(index, 16);
    }

    public static boolean canCombineStacks(ItemStack first, ItemStack second) {
        String firstId   = getTokenID(first);
        String secondId  = getTokenID(second);
        boolean firstNF  = isNonFungible(first);
        boolean secondNF = isNonFungible(second);

        if (StringUtils.isEmpty(firstId)
                || StringUtils.isEmpty(secondId)
                || firstNF
                || secondNF
                || !firstId.equals(secondId))
            return false;

        int maxStackSize = first.getMaxStackSize();
        return maxStackSize == second.getMaxStackSize()
                && first.getType() == second.getType()
                && first.getAmount() + second.getAmount() <= maxStackSize;
    }

}
