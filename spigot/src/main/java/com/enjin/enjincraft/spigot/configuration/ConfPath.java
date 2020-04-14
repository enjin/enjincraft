package com.enjin.enjincraft.spigot.configuration;

public enum ConfPath {

    BASE_URL("platform.base-url"),
    APP_ID("platform.app.id"),
    APP_SECRET("platform.app.secret"),
    DEV_IDENTITY_ID("platform.app.dev-identity-id"),
    LOCALE("locale"),
    DEBUG_SDK("debug.sdk"),
    DEBUG_PLUGIN("debug.plugin"),
    SENTRY_URL("debug.sentry"),
    TRANSLATE_CONSOLE_MESSAGES("experimental.translate-console-messages"),
    PERMISSION_BLACKLIST("permission-blacklist");

    private String path;

    ConfPath(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

}
