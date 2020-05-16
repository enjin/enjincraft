package com.enjin.enjincraft.translations;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum Locale {

    da_DK(StandardCharsets.UTF_16BE),
    de_DE(StandardCharsets.UTF_16BE),
    en_US(StandardCharsets.UTF_8),
    es_ES(StandardCharsets.UTF_16BE),
    fil_PH(StandardCharsets.UTF_16BE),
    ja_JP(StandardCharsets.UTF_16BE),
    ko_KR(StandardCharsets.UTF_16BE),
    pt_BR(StandardCharsets.UTF_16BE),
    ro_RO(StandardCharsets.UTF_16BE),
    sr_SP(StandardCharsets.UTF_16BE);

    private Charset charset;

    Locale(Charset charset) {
        this.charset = charset;
    }

    public String locale() {
        return name();
    }

    public Charset charset() {
        return charset;
    }

    public static Locale of(String name) {
        for (Locale locale : values()) {
            if (locale.name().equalsIgnoreCase(name))
                return locale;
        }

        return null;
    }
}
