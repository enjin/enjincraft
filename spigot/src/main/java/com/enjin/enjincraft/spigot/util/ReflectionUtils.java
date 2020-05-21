package com.enjin.enjincraft.spigot.util;

import lombok.SneakyThrows;

import java.lang.reflect.Field;

public final class ReflectionUtils {

    private ReflectionUtils() {
        throw new IllegalStateException();
    }

    @SneakyThrows
    public static Field getDeclaredField(Class clazz, String name) {
        return clazz.getDeclaredField(name);
    }

}
