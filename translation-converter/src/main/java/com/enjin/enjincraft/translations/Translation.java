package com.enjin.enjincraft.translations;

import lombok.Getter;
import lombok.NonNull;

import java.util.*;

@Getter
public class Translation {

    public static final String ROOT = "";

    private final String locale;
    private final String language;
    private final Integer version;

    private final TranslationEntry root = new TranslationEntry(ROOT);

    private Translation() {
        throw new IllegalStateException();
    }

    public Translation(@NonNull String locale,
                       @NonNull String language,
                       @NonNull Integer version) {
        this.locale   = locale;
        this.language = language;
        this.version  = version;
    }

    public boolean addEntry(String path, String entry) {
        List<String> keys = new ArrayList<>(Arrays.asList(path.split(TranslationEntry.PATH_SEPARATOR)));

        if (keys.size() == 0)
            return false;

        return this.root.addEntry(keys, entry);
    }

}
