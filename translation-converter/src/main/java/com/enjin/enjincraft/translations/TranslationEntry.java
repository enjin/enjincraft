package com.enjin.enjincraft.translations;

import lombok.Getter;
import lombok.NonNull;

import java.util.*;

@Getter
public class TranslationEntry {

    public static final String PATH_SEPARATOR = "\\.";
    public static String EMPTY_ENTRY = "";

    private final String entry;
    private final String key;
    private final Map<String, TranslationEntry> entries = new HashMap<>();

    private TranslationEntry() {
        throw  new IllegalStateException();
    }

    public TranslationEntry(@NonNull String key) {
        this(key, EMPTY_ENTRY);
    }

    public TranslationEntry(@NonNull String key, @NonNull String entry) {
        this.key   = key;
        this.entry = entry;
    }

    public boolean addEntry(List<String> keys, String entry) {
        // Prevents entries at the root level
        if (keys.size() == 0)
            return false;

        String key = keys.remove(0);

        if (!isKeyValid(key))
            return false;

        TranslationEntry next = entries.get(key);

        if (next == null) {
            if (keys.size() > 0) {
                next = new TranslationEntry(key);
                entries.put(key, next);
                return next.addEntry(keys, entry);
            }

            next = new TranslationEntry(key, entry);
            entries.put(key, next);
            return true;
        }

        // Prevents entries from being added at the leaf of a tree
        if (keys.size() == 0)
            return false;

        return next.addEntry(keys, entry);
    }

    private boolean isKeyValid(String key) {
        if (key.isEmpty())
            return false;

        for (char ch : key.toCharArray()) {
            if (!((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')))
                return false;
        }

        return true;
    }

}
