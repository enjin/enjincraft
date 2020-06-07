package com.enjin.enjincraft.spigot.util;

public final class PlayerUtils {

    public static final int MAX_USERNAME_LENGTH = 16;
    public static final int MIN_USERNAME_LENGTH = 3;

    private PlayerUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isValidUserName(String name) {
        return name.length() >= MIN_USERNAME_LENGTH && name.length() <= MAX_USERNAME_LENGTH;
    }
    
}
