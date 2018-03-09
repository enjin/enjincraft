package io.enjincoin.spigot_framework.util;

import java.math.BigInteger;
import java.util.UUID;

public class UuidUtil {

    public static final int UUID_LENGTH = 36;
    public static final int STRIPPED_UUID_LENGTH = 32;

    public static UUID stringToUuid(String s) {
        if (s != null & !s.isEmpty()) {
            String stripped = s.replaceAll("-", "");
            if (stripped.length() == 32) {
                BigInteger least = new BigInteger(s.substring(0, 16), 16);
                BigInteger most = new BigInteger(s.substring(16, 32), 16);
                return new UUID(least.longValue(), most.longValue());
            }
        }
        return null;
    }

}
