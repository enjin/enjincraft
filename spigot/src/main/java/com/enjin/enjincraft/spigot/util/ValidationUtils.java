package com.enjin.enjincraft.spigot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationUtils {

    public static final Pattern TOKEN_ID_PATTERN = Pattern.compile("^(?:0[xX])?([0-9a-fA-F]{16})$");
    public static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]+$");

    public static boolean isValidTokenId(String input) {
        return TOKEN_ID_PATTERN.matcher(input).matches();
    }

    public static String getValidTokenId(String input) {
        Matcher matcher = TOKEN_ID_PATTERN.matcher(input);
        return matcher.matches() ? matcher.group(1) : null;
    }

    public static boolean isValidNumber(String input) {
        return NUMBER_PATTERN.matcher(input).matches();
    }

    public static Pattern getAlphaNumericPattern(int min) {
        return Pattern.compile(String.format("^[a-zA-Z0-9]{%s,}$", min));
    }

}
