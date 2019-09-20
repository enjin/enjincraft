package com.enjin.ecmp.spigot.i18n;

import java.nio.charset.Charset;

public enum  Locale {

    da_DK("Dansk", "UTF-8"),
    de_DE("Deutsch", "UTF-8"),
    en_US("English", "UTF-8"),
    fil_PH("Pilipino", "UTF-8"),
    ko_KR("Korean", "UTF-16BE"),
    pt_BR("Português", "UTF-8"),
    sr_SA("Serbian", "UTF-8");

    private String language;
    private Charset charset;

    Locale(String language, String charset) {
        this.language = new String(language.getBytes(), Charset.forName("UTF-8"));
        this.charset = Charset.forName(charset);
    }

    public String locale() {
        return name();
    }

    public String language() {
        return language;
    }

    public Charset charset() {
        return charset;
    }

    public static Locale of(String name) {
        for (Locale locale : values()) {
            if (locale.name().equalsIgnoreCase(name) || locale.language().equalsIgnoreCase(name))
                return locale;
        }

        return en_US;
    }
}
