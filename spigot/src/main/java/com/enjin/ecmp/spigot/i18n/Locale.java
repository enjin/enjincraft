package com.enjin.ecmp.spigot.i18n;

import java.nio.charset.Charset;

public enum  Locale {

    da_DK("Dansk"),
    de_DE("Deutsche"),
    en_US("English"),
    es_ES("Española"),
    fil_PH("Pilipino"),
    pt_BR("Português");

    private String language;

    Locale(String language) {
        this.language = new String(language.getBytes(), Charset.forName("UTF-8"));
    }

    public String locale() {
        return name();
    }

    public String language() {
        return language;
    }
}
