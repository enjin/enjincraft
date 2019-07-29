package com.enjin.ecmp.spigot_framework.util;

import java.math.BigInteger;
import java.util.UUID;

/**
 * <p>Operations on {@link UUID}.</p>
 *
 * <p>This class tries to handle {@code null} input gracefully.
 * An exception will generally not be thrown for a {@code null} input.
 * Each method documents its behaviour in detail.</p>
 *
 * <p>#ThreadSafe#</p>
 *
 * @since 1.0
 */
public class UuidUtils {

    /**
     * <p>The length of a serialized UUID with hyphens.</p>
     */
    public static final int UUID_LENGTH = 36;

    /**
     * <p>The length of a serialized UUID without hyphens.</p>
     */
    public static final int STRIPPED_UUID_LENGTH = 32;

    /**
     * <p>The radix used to construct the least and most
     * significant bit big integers of a UUID.</p>
     */
    public static final int RADIX = 16;

    /**
     * <p>{@code UuidUtils} instances should NOT be constructed in standard programming.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
     */
    public UuidUtils() {
        super();
    }

    /**
     * <p>Converts a serialized UUID to an instance of {@link UUID}.</p>
     *
     * @param s the serialized uuid
     * @return {@link UUID} instance of the serialized UUID
     * @since 1.0
     */
    public static UUID stringToUuid(String s) {
        if (s != null & !s.isEmpty()) {
            String stripped = s.replaceAll("-", "");
            if (stripped.length() == STRIPPED_UUID_LENGTH) {
                BigInteger least = new BigInteger(stripped.substring(0, RADIX), RADIX);
                BigInteger most = new BigInteger(stripped.substring(RADIX, STRIPPED_UUID_LENGTH), RADIX);
                return new UUID(least.longValue(), most.longValue());
            }
        }
        return null;
    }

}
