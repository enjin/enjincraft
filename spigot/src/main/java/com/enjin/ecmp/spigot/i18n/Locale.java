package com.enjin.ecmp.spigot.i18n;

public enum  Locale {

    en_US("English"),
    es_ES("Española"),
    da_DK("Dansk");

    private String language;

    Locale(String language) {
        this.language = language;
    }

    public String locale() {
        return name();
    }

    public String language() {
        return language;
    }
}
