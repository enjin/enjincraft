package com.enjin.ecmp.spigot.i18n;

import java.nio.charset.Charset;

public enum  Locale {

    da_DK("Dansk"),
    de_DE("Deutsch"),
    en_US("English"),
    fil_PH("Pilipino"),
    ko_KR("Korean"),
    pt_BR("Português"),
    sr_SA("Serbian");

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
